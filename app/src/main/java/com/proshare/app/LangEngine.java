package com.proshare.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

public class LangEngine {

    private static final String PREF = "HyperLangPref";
    public static final String[] CODES = {"en", "hi", "bn", "mr", "te", "ta", "gu", "pa", "es", "ar", "ru"};
    public static final String[] NAMES = {
            "🌐 English", "🇮🇳 हिंदी (Hindi)", "🇮🇳 বাংলা (Bengali)", "🇮🇳 मराठी (Marathi)",
            "🇮🇳 తెలుగు (Telugu)", "🇮🇳 தமிழ் (Tamil)", "🇮🇳 ગુજરાતી (Gujarati)",
            "🇮🇳 ਪੰਜਾਬੀ (Punjabi)", "🇪🇸 Español", "🇸🇦 العربية (Arabic)", "🇷🇺 Русский"
    };

    public static int getLangIndex(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!sp.contains("lang_idx")) {
            // Auto-detect phone language on first launch
            String sys = Locale.getDefault().getLanguage();
            for (int i = 0; i < CODES.length; i++) {
                if (CODES[i].equalsIgnoreCase(sys)) {
                    sp.edit().putInt("lang_idx", i).apply();
                    return i;
                }
            }
        }
        return sp.getInt("lang_idx", 0);
    }

    public static void setLangIndex(Context ctx, int idx) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt("lang_idx", idx % NAMES.length).apply();
    }

    public static void nextLanguage(Context ctx) {
        setLangIndex(ctx, (getLangIndex(ctx) + 1) % NAMES.length);
    }

    public static String getCurrentLangName(Context ctx) {
        return NAMES[getLangIndex(ctx)];
    }

    public static String t(Context ctx, String key) {
        int i = getLangIndex(ctx);
        if ("SEND".equals(key)) {
            String[] v = {"SEND", "भेजें (Send)", "পাঠান", "पाठवा", "పంపు", "அனுப்பு", "મોકલો", "ਭੇਜੋ", "ENVIAR", "إرسال", "ОТПРАВИТЬ"};
            return v[i];
        }
        if ("RECEIVE".equals(key)) {
            String[] v = {"RECEIVE", "प्राप्त करें", "গ্রহণ করুন", "स्वीकारा", "స్వీకరించు", "பெறு", "મેળવો", "ਪ੍ਰਾਪਤ ਕਰੋ", "RECIBIR", "استلام", "ПОЛУЧИТЬ"};
            return v[i];
        }
        if ("ONE_HAND".equals(key)) {
            String[] v = {"✨ ONE-HAND MODE", "✨ एक हाथ मोड (One-Hand)", "✨ এক হাত মোড", "✨ एक हात मोड", "✨ వన్-హ్యాండ్ మోడ్", "✨ ஒரு கை முறை", "✨ વન-હેન્ડ મોડ", "✨ ਵਨ-ਹੈਂਡ ਮੋਡ", "✨ MODO UNA MANO", "✨ وضع اليد الواحدة", "✨ ОДНОЙ РУКОЙ"};
            return v[i];
        }
        if ("SUPER_TOOLS".equals(key)) {
            String[] v = {"🛠️ Super Tools & Phone Clone", "🛠️ सुपर टूल्स व फोन क्लोन", "🛠️ সুপার টুলস", "🛠️ सुपर टूल्स", "🛠️ సూపర్ టూల్స్", "🛠️ சூப்பர் கருவிகள்", "🛠️ સુપર ટૂલ્સ", "🛠️ ਸੁਪਰ ਟੂਲਸ", "🛠️ Súper Herramientas", "🛠️ أدوات خارقة", "🛠️ Супер Инструменты"};
            return v[i];
        }
        if ("HISTORY".equals(key)) {
            String[] v = {"🕒 History Vault", "🕒 हिस्ट्री वॉल्ट", "🕒 হিস্ট্রি", "🕒 हिस्ट्री", "🕒 హిస్టరీ", "🕒 வரலாறு", "🕒 હિસ્ટ્રી", "🕒 ਹਿਸਟਰੀ", "🕒 Historial", "🕒 السجل", "🕒 История"};
            return v[i];
        }
        return key;
    }
}
