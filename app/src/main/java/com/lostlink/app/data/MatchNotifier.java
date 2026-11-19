package com.lostlink.app.data;

import android.content.ContentResolver;
import android.content.Context;

import com.lostlink.app.R;

import java.util.ArrayList;
import java.util.Locale;

public final class MatchNotifier {
    private MatchNotifier() { }

    public static ArrayList<MatchFinder.MatchResult> notifyMatches(Context context,
                                                                   LostLinkDatabase database,
                                                                   LostItem source) {
        ArrayList<MatchFinder.MatchResult> matches =
                new MatchFinder().findMatches(database, context.getContentResolver(), source);
        for (MatchFinder.MatchResult match : matches) {
            LostItem other = match.item;
            int confidence = (int) Math.round(match.score * 100.0);
            String message = context.getString(R.string.match_notification_message,
                    source.name, other.name, confidence);
            database.addNotification(source.reporterEmail,
                    context.getString(R.string.possible_match_title), message, source.id);
            LocalNotifier.showForSignedInUser(context, source.reporterEmail,
                    context.getString(R.string.possible_match_title), message, source.id);
            if (!other.reporterEmail.equals(source.reporterEmail)) {
                database.addNotification(other.reporterEmail,
                        context.getString(R.string.possible_match_title), message, other.id);
                LocalNotifier.showForSignedInUser(context, other.reporterEmail,
                        context.getString(R.string.possible_match_title), message, other.id);
            }
        }
        return matches;
    }
}
