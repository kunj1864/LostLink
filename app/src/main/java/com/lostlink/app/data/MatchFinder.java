package com.lostlink.app.data;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class MatchFinder {
    public ArrayList<MatchResult> findMatches(LostLinkDatabase database,
                                              ContentResolver resolver, LostItem source) {
        String oppositeType = "LOST".equals(source.type) ? "FOUND" : "LOST";
        ArrayList<LostItem> candidates = database.findItems(oppositeType, null, null, null);
        ArrayList<MatchResult> matches = new ArrayList<>();
        for (LostItem candidate : candidates) {
            if ("Resolved".equals(candidate.status)) continue;
            double image = imageSimilarity(resolver, source.photoUri, candidate.photoUri);
            double text = tokenSimilarity(source.name + " " + source.description,
                    candidate.name + " " + candidate.description);
            double location = tokenSimilarity(locationText(source), locationText(candidate));
            double category = source.category.equalsIgnoreCase(candidate.category) ? 1.0 : 0.0;
            boolean exactReportMatch = source.name.equalsIgnoreCase(candidate.name)
                    && source.category.equalsIgnoreCase(candidate.category)
                    && sameNonEmpty(source.district, candidate.district)
                    && sameNonEmpty(source.taluka, candidate.taluka);
            boolean bothHavePhotos = source.photoUri != null && candidate.photoUri != null;
            double score = bothHavePhotos
                    ? (category * 0.18) + (text * 0.22) + (location * 0.15)
                    + (image * 0.35) + (exactReportMatch ? 0.10 : 0.0)
                    : (category * 0.25) + (text * 0.45) + (location * 0.20)
                    + (exactReportMatch ? 0.10 : 0.0);
            boolean visualMatch = bothHavePhotos && image >= 0.88 && score >= 0.30;
            boolean descriptiveMatch = text >= 0.45
                    && ((category > 0 && score >= 0.40) || score >= 0.45);
            boolean locationMatch = category > 0 && location >= 0.35 && score >= 0.40;
            if (exactReportMatch || visualMatch || descriptiveMatch || locationMatch) {
                matches.add(new MatchResult(candidate, score, image));
            }
        }
        Collections.sort(matches, Comparator.comparingDouble(
                (MatchResult result) -> result.score).reversed());
        if (matches.size() > 5) {
            return new ArrayList<>(matches.subList(0, 5));
        }
        return matches;
    }

    private static double tokenSimilarity(String first, String second) {
        Set<String> left = words(first);
        Set<String> right = words(second);
        if (left.isEmpty() || right.isEmpty()) return 0.0;
        int intersection = 0;
        for (String token : left) {
            if (right.contains(token)) intersection++;
        }
        return (2.0 * intersection) / (left.size() + right.size());
    }

    private static Set<String> words(String value) {
        HashSet<String> result = new HashSet<>();
        if (value == null) return result;
        for (String token : value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.length() > 2) result.add(token);
        }
        return result;
    }

    private static String locationText(LostItem item) {
        return item.location + " " + item.area + " " + item.district + " " + item.taluka;
    }

    private static boolean sameNonEmpty(String first, String second) {
        return first != null && !first.trim().isEmpty() && second != null
                && first.trim().equalsIgnoreCase(second.trim());
    }

    private static double imageSimilarity(ContentResolver resolver, String firstUri, String secondUri) {
        if (firstUri == null || secondUri == null) return 0.0;
        boolean[] first = imageAverageHash(resolver, firstUri);
        boolean[] second = imageAverageHash(resolver, secondUri);
        if (first == null || second == null) return 0.0;
        int equalBits = 0;
        for (int i = 0; i < first.length; i++) {
            if (first[i] == second[i]) equalBits++;
        }
        return equalBits / (double) first.length;
    }

    private static boolean[] imageAverageHash(ContentResolver resolver, String uriString) {
        Bitmap bitmap = null;
        Bitmap scaled = null;
        try (InputStream stream = resolver.openInputStream(Uri.parse(uriString))) {
            if (stream == null) return null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 16;
            bitmap = BitmapFactory.decodeStream(stream, null, options);
            if (bitmap == null) return null;
            scaled = Bitmap.createScaledBitmap(bitmap, 8, 8, true);
            double[] brightness = new double[64];
            double total = 0.0;
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int color = scaled.getPixel(x, y);
                    double gray = (0.299 * ((color >> 16) & 0xff))
                            + (0.587 * ((color >> 8) & 0xff))
                            + (0.114 * (color & 0xff));
                    brightness[y * 8 + x] = gray;
                    total += gray;
                }
            }
            double average = total / brightness.length;
            boolean[] hash = new boolean[brightness.length];
            for (int i = 0; i < brightness.length; i++) hash[i] = brightness[i] >= average;
            return hash;
        } catch (IOException | SecurityException | IllegalArgumentException exception) {
            return null;
        } finally {
            if (scaled != null && scaled != bitmap) scaled.recycle();
            if (bitmap != null) bitmap.recycle();
        }
    }

    public static final class MatchResult {
        public final LostItem item;
        public final double score;
        public final double imageScore;

        MatchResult(LostItem item, double score, double imageScore) {
            this.item = item;
            this.score = score;
            this.imageScore = imageScore;
        }
    }
}
