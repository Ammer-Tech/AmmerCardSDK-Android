package com.example.sample.ingenico.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    private Util() {

    }

    public static String generateUniqueID() {
        long timestamp = System.currentTimeMillis();

        String timestampAsString = Long.toString(timestamp);

        // Take the last 8 digits of the timestamp to ensure uniqueness
//        String uniquePart = timestampAsString.substring(timestampAsString.length() - 8);

        // Alternatively, you can use a random generator for extra randomness (optional)
         Random random = new Random(timestamp);
         int randomInt = 10000000 + random.nextInt(90000000); // Ensures the random number is 8 digits
         String uniquePart = Integer.toString(randomInt);

        // Concatenate the prefix with the unique part
        return "INGTX" + uniquePart;
    }

    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    public static boolean isValidDecimal(CharSequence input) {
        Pattern pattern = Pattern.compile("^\\d{1,}(\\.\\d{1,2})?$");
        Matcher matcher = pattern.matcher(input);
        return matcher.matches();
    }

    public static Drawable loadDrawableFromUrl(Context context, String url) {
        try {
            Bitmap x;
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.connect();
            try (InputStream input = connection.getInputStream()) {
                x = BitmapFactory.decodeStream(input);
                return new BitmapDrawable(context.getResources(), x);
            }
        } catch (Exception e) {
            Log.e(TAG, "loadDrawableFromUrl -> " + url + " " + e.getMessage());
            return null;
        }

    }

    public static Drawable loadDrawableFromAssets(Context context, String fileName) {
        try (InputStream is = context.getAssets().open(fileName)) {
            return Drawable.createFromStream(is, null);
        } catch (IOException e) {
            Log.e(TAG, "loadDrawableFromAssets -> " + e.getMessage());
            return null;
        }
    }

    public static Drawable loadDrawableFromAssets(Context context, String fileName, int widthDp, int heightDp) {
        try (InputStream is = context.getAssets().open(fileName)) {
            // Convert dp to pixel
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int widthPx = (int) (widthDp * metrics.density + 0.5f);
            int heightPx = (int) (heightDp * metrics.density + 0.5f);
            // Decode the input stream to a bitmap
            Bitmap originalBitmap = BitmapFactory.decodeStream(is);
            // Resize the bitmap
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, widthPx, heightPx, true);
            // Convert back to drawable and return
            return new BitmapDrawable(context.getResources(), resizedBitmap);
        } catch (IOException e) {
            Log.e(TAG, "loadDrawableFromAssets -> " + e.getMessage());
            return null;
        }
    }

    public static JSONObject loadJSONFromAssets(Context context, String fileName) {
        String json;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
            return new JSONObject(json);
        } catch (IOException | JSONException ex) {
Log.e("Util", "loadJSONFromAsset: " + ex.getMessage());
            return null;
        }
    }

    public static String getFormattedDateUTC() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
            // Format it as an ISO 8601 string
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            return now.format(formatter);
        }
        return "Error getting formatted date";
    }

    public static SpannableStringBuilder getFormattedSpannableString(String baseString, String... values) {
        SpannableStringBuilder spannableBuilder = new SpannableStringBuilder(baseString);

        // Track the index where the next value should be inserted.
        int index = 0;

        for (String value : values) {
            // Find the placeholder index in the base string.
            index = spannableBuilder.toString().indexOf("%s", index);

            if (index == -1) {
                // No placeholder found, break the loop.
                break;
            }

            // Create a spannable string with bold style for the value.
            SpannableString spannableValue = new SpannableString(value);
            spannableValue.setSpan(new StyleSpan(Typeface.BOLD), 0, value.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Replace the placeholder with our styled value.
            spannableBuilder.replace(index, index + 2, spannableValue);

            // Move the index forward, past the inserted value.
            index += value.length();
        }

        return spannableBuilder;
    }

    public static String str2Readable(String strBalance) {
        if (strBalance.equals("0")) {
            return "0.00";
        }
        String temp = strBalance.substring(0, strBalance.length() - 16);
        return String.format(new Date().getTime() > 0 ? "%.2f" : "%s", Double.parseDouble(temp) / 100);
    }

    public static String getEmojiByUnicode(int unicode){
        return new String(Character.toChars(unicode));
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Log.e(TAG, "sleep -> " + e.getMessage());
        }
    }


    public static final String TAG = "Util";
}
