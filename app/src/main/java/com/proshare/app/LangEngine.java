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
            // Auto-detect phone's default language on first launch
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
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt("lang_idx", idx).apply();
    }

    public static String getCurrentLangName(Context ctx) {
        return NAMES[getLangIndex(ctx)];
    }

    public static String t(Context ctx, String key) {
        int i = getLangIndex(ctx);
        switch (key) {
            case "SEND":
                return new String[]{"SEND", "भेजें (Send)", "পাঠান", "पाठवा", "పంపు", "அனுப்பு", "મોકલો", "ਭੇਜੋ", "ENVIAR", "إرسال", "ОТПРАВИТЬ"}[i];
            case "RECEIVE":
                return new String[]{"RECEIVE", "प्राप्त करें", "গ্রহণ করুন", "स्वीकारा", "స్వీకరించు", "பெறு", "મેળવો", "ਪ੍ਰਾਪਤ ਕਰੋ", "RECIBIR", "استلام", "ПОЛУЧИТЬ"}[i];
            case "HISTORY":
                return new String[]{"🕒 History Vault", "🕒 हिस्ट्री वॉल्ट", "🕒 হিস্ট্রি", "🕒 हिस्ट्री", "🕒 హిస్టరీ", "🕒 வரலாறு", "🕒 હિસ્ટ્રી", "🕒 ਹਿਸਟਰੀ", "🕒 Historial", "🕒 السجل", "🕒 История"}[i];
            case "PC_CONNECT":
                return new String[]{"💻 TV / PC Hub", "💻 टीवी / पीसी हब", "💻 টিভি/পিসি", "💻 टीव्ही/पीसी", "💻 టీవీ/పీసీ", "💻 டிவி/பிசி", "💻 ટીવી/પીસી", "💻 ਟੀਵੀ/ਪੀਸੀ", "💻 TV / PC", "💻 التلفاز والكمبيوتر", "💻 ТВ и ПК"}[i];
            case "ONE_HAND":
                return new String[]{"🖐️ ONE-HAND MODE", "🖐️ एक हाथ मोड (One-Hand)", "🖐️ এক হাত মোড", "🖐️ एक हात मोड", "🖐️ వన్-హ్యాండ్", "🖐️ ஒரு கை முறை", "🖐️ વન-હેન્ડ મોડ", "🖐️ ਵਨ-ਹੈਂਡ ਮੋਡ", "🖐️ UNA MANO", "🖐️ وضع اليد الواحدة", "🖐️ ОДНОЙ РУКОЙ"}[i];
            case "SUPER_TOOLS":
                return new String[]{"🛠️ Super Tools & Phone Clone", "🛠️ सुपर टूल्स और फोन क्लोन", "🛠️ সুপার টুলস ও ক্লোন", "🛠️ सुपर टूल्स व क्लोन", "🛠️ సూపర్ టూల్స్", "🛠️ சூப்பர் கருவிகள்", "🛠️ સુપર ટૂલ્સ", "🛠️ ਸੁਪਰ ਟੂਲਸ", "🛠️ Súper Herramientas", "🛠️ أدوات واستنساخ", "🛠️ Супер Инструменты"}[i];
            case "SETTINGS":
                return new String[]{"⚙️ Settings & Themes", "⚙️ सेटिंग्स, भाषा और थीम्स", "⚙️ সেটিংস ও থিম", "⚙️ सेटिंग्ज आणि थीम", "⚙️ సెట్టింగ్‌లు", "⚙️ அமைப்புகள்", "⚙️ સેટિંગ્સ", "⚙️ ਸੈਟਿੰਗਾਂ", "⚙️ Ajustes y Temas", "⚙️ الإعدادات والسمات", "⚙️ Настройки и Темы"}[i];
            default:
                return key;
        }
    }
}
