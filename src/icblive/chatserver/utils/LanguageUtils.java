package icblive.chatserver.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.google.common.io.Files;

public class LanguageUtils {
	// 国际版weboscket的多语言支持
	private static LanguageUtils instance = null;
	private static Logger logger = LogManager.getLogger(LanguageUtils.class.getName());
	private Map<String, String> vocabulary;
	private String[] supportLangs;
	public static LanguageUtils getInstance() {
		if (instance == null) {
			instance = new LanguageUtils();
		}
		return instance;
	}
	private LanguageUtils() {
		this.vocabulary = new HashMap<String, String>();
		this.supportLangs = new String[]{"en_lang","ch_lang","es_lang", "hi_lang"};
	}
	public String[] getAllLanguage() {
		return this.supportLangs;
	}
	public boolean initLanguageCont() {
		File file = new File("./lang.config.json");
		if (file.exists()) {
			try {
				JSONObject values = JSON.parseObject(Files.toString(file, Charset.forName("UTF-8")));
				String s_l = values.getString("support_languages");
				if (Strings.isNullOrEmpty(s_l)) {
					logger.debug("not contons key support_languages");
					return false;
				}
				this.supportLangs = s_l.split(",");
				for(String lang : this.supportLangs) {
					String path = values.getString(lang);
					if (Strings.isNullOrEmpty(path)) {
						logger.debug("no lang configfile" + lang);
						continue;
					}
					File tmpfile = new File(path);
					if (tmpfile.exists()) {
						JSONObject langvalues = JSON.parseObject(Files.toString(tmpfile, Charset.forName("UTF-8")));
						Set<String> lvalues = langvalues.keySet();
						for(String s : lvalues) {
							String key = lang + "_" + s;
							this.vocabulary.put(key, langvalues.getString(s));
						}
					} else {
						logger.debug("lang configfile not exists" + lang);
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			logger.debug("file not exists:lang.config.json");
			return false;
		}
		logger.debug(this.toString());
		return true;
		
	}
	public String isSupportLangs(int langtype) {
		if (this.supportLangs == null || this.supportLangs.length == 0) {
			return "en";
		}
		if (this.supportLangs.length < langtype) {
			return supportLangs[0];
		}
		return this.supportLangs[langtype];
	}
	public String getContents(String typeLang, String keyLang) {
		String value = this.vocabulary.get(typeLang + "_" + keyLang);
		if (Strings.isNullOrEmpty(value)) {
			return "";
		}
		return value;
	}
	@Override
	public String toString() {
		return "LanguageUtils [vocabulary=" + vocabulary + ", supportLangs=" + Arrays.toString(supportLangs) + "]";
	}
}
