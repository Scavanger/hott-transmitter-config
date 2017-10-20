package gde.model.voice;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum VoiceRssLanguage {
    ca_es, zh_cn, zh_hk, zh_tw, da_dk, nl_nl, en_au, en_ca, en_gb, en_in, en_us, fi_fi, fr_ca, fr_fr, de_de, it_it, ja_jp, ko_kr, nb_no, pl_pl, pt_br, pt_pt,
    ru_ru, es_mx, es_es, sv_se;

    public static VoiceRssLanguage forString(final String language) {
        return VoiceRssLanguage.valueOf(language.toLowerCase().replace("-", "_"));
    }

    public static VoiceRssLanguage forString(final String language, final String variant) {
        return VoiceRssLanguage.valueOf(String.format("%s_%s", language, variant).toLowerCase());
    }

    public static List<String> getAll() {
        return Stream.of(values()).map(l -> l.toString()).collect(Collectors.toList());
    }

    public String getKey() {
        return name().toLowerCase().replace("_", "-");
    }

    @Override
    public String toString() {
        return ResourceBundle.getBundle(getClass().getName()).getString(name());
    }
}
