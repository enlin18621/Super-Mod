package com.nimbus.weather;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;

final class L10n {
    private static final Map<String,String[]> TERMS = new HashMap<>();
    private static void add(String key, String de, String en, String zh) { TERMS.put(key,new String[]{de,en,zh}); }
    static {
        add("app","NIMBUS · DEIN TAG","NIMBUS · YOUR DAY","NIMBUS · 今日");
        add("weather","Wetter","Weather","天氣");
        add("feels","Gefühlt","Feels like","體感");
        add("today","Heute","Today","今天");
        add("high","Max","High","最高");
        add("low","Min","Low","最低");
        add("rain","Regenrisiko","Rain chance","降雨機率");
        add("wind","Wind","Wind","風速");
        add("humidity","Luftfeuchtigkeit","Humidity","濕度");
        add("uv","UV-Index","UV index","紫外線");
        add("sunrise","Sonnenaufgang","Sunrise","日出");
        add("sunset","Sonnenuntergang","Sunset","日落");
        add("calendar","Mein Kalender","My calendar","我的行事曆");
        add("calempty","Heute keine Termine gefunden.","No events found today.","今天沒有找到行程。");
        add("calpermission","Kalenderzugriff erlauben","Allow calendar access","允許讀取行事曆");
        add("departures","Nächste Abfahrten","Next departures","即將發車");
        add("stopmissing","Haltestelle in den Einstellungen eingeben.","Set a stop in Settings.","請於設定中輸入車站。");
        add("departuresempty","Keine Abfahrten verfügbar.","No departures available.","目前沒有發車資訊。");
        add("brief","Das solltest du mitnehmen","What to bring","今日攜帶建議");
        add("edit","Einstellungen","Settings","設定");
        add("city","Wetterort","Weather city","天氣城市");
        add("home","Zuhause (nur auf diesem Gerät)","Home address (this device only)","住家地址（僅儲存在此裝置）");
        add("uni","Uni/Zieladresse (nur auf diesem Gerät)","University/destination (this device only)","大學／目的地（僅儲存在此裝置）");
        add("stop","VBB-Haltestelle (Berlin/Brandenburg)","VBB stop (Berlin/Brandenburg)","VBB 車站（柏林／布蘭登堡）");
        add("plans","Meine Tagespläne (eine Zeile pro Plan)","My plans today (one per line)","今日計畫（每行一項）");
        add("lang","Sprache","Language","語言");
        add("fahrenheit","Fahrenheit verwenden (°F)","Use Fahrenheit (°F)","使用華氏溫度 (°F)");
        add("shareaddress","Private Adressen in ChatGPT teilen (optional)","Include private addresses in ChatGPT share (optional)","分享至 ChatGPT 時包含私人地址（選用）");
        add("save","Änderungen speichern","Save changes","儲存變更");
        add("refresh","Alles aktualisieren","Refresh everything","更新全部");
        add("yr","Wetterbericht bei Yr ↗","Forecast at Yr ↗","前往 Yr 查看天氣 ↗");
        add("chatgpt","Mit ChatGPT planen ↗","Plan with ChatGPT ↗","使用 ChatGPT 規劃 ↗");
        add("paste","ChatGPT-Update aus Zwischenablage","Import ChatGPT update from clipboard","從剪貼簿匯入 ChatGPT 更新");
        add("pasteEmpty","Kein NIMBUS_UPDATE in der Zwischenablage gefunden.","No NIMBUS_UPDATE found on clipboard.","剪貼簿找不到 NIMBUS_UPDATE。");
        add("chatnote","Öffnet ChatGPT mit deinem Tageskontext. KI-Antworten werden nicht automatisch ins Widget übernommen.","Shares today's context with ChatGPT. AI replies are not automatically imported into the widget.","將今日資訊傳給 ChatGPT。AI 回覆不會自動匯入小工具。");
        add("updating","Aktualisiere Wetter, Termine und Verkehr …","Refreshing weather, calendar and transport …","正在更新天氣、行事曆與交通…");
        add("saved","Einstellungen gespeichert.","Settings saved.","設定已儲存。");
        add("error","Aktualisierung teilweise fehlgeschlagen; vorhandene Daten werden angezeigt.","Some sources failed; showing cached data where possible.","部分資料更新失敗，會盡可能顯示快取。");
        add("updated","Stand","Updated","更新時間");
        add("bringBasics","Handy, Schlüssel und Geldbeutel","Phone, keys and wallet","手機、鑰匙與錢包");
        add("bringRain","Regenschirm oder Regenjacke","Umbrella or rain jacket","雨傘或防水外套");
        add("bringCold","Warme Jacke und eine zusätzliche Schicht","Warm jacket and an extra layer","保暖外套與多一件衣服");
        add("bringGloves","Handschuhe und Mütze","Gloves and a hat","手套與毛帽");
        add("bringSun","Sonnencreme und Wasser","Sunscreen and water","防曬乳與水");
        add("bringUni","Studentenausweis, Laptop, Ladegerät und Unterlagen","Student ID, laptop, charger and course materials","學生證、筆電、充電器與課堂資料");
        add("bringSports","Sportkleidung, Sportschuhe und Trinkflasche","Sportswear, trainers and water bottle","運動服、運動鞋與水壺");
        add("bringOutdoor","Bequeme Schuhe, Wasser und passende Kleidung","Comfortable shoes, water and suitable clothing","舒適的鞋子、水與合適衣物");
        add("bringTravel","Ticket, Ausweis, Ladegerät und rechtzeitig losfahren","Ticket, ID, charger and leave plenty of time","車票、身分證件、充電器並提早出門");
        add("bringBattery","Powerbank bei längerem Tag","Power bank for a long day","長時間外出攜帶行動電源");
        add("ruleDisclaimer","Hinweise nach Wetter und Stichworten, keine KI-Analyse.","Suggestions based on weather and keywords, not AI analysis.","建議依天氣與關鍵字產生，非 AI 分析。");
        add("at","um","at","於");
        add("allday","Ganztägig","All day","全天");
        add("late","Verspätung","Delay","延誤");
        add("settingsHint","Deine Adressen und Pläne bleiben lokal auf deinem Handy. Für einen neuen Wetterort bitte Stadt eingeben.","Addresses and plans stay on your phone. Enter a city to change weather location.","地址與計畫僅存於手機。輸入城市名稱可切換天氣地點。");
        add("map","Route zur Uni in Maps ↗","Route to university in Maps ↗","在地圖中規劃前往大學路線 ↗");
        add("importTitle","Nimbus-Update importieren?","Import Nimbus update?","匯入 Nimbus 更新？");
        add("imported","Update importiert. Bitte neue Wetterorte über Speichern prüfen.","Update imported. Please verify new weather cities with Save.","已匯入更新。若更換城市，請按儲存確認。");
        add("cancel","Abbrechen","Cancel","取消");
        add("import","Importieren","Import","匯入");
        add("permission","Kalender erlauben, um deine heutigen Termine einzublenden.","Allow calendar access to display today's events.","允許行事曆權限以顯示今日活動。");
        add("nextEvent","Nächster Termin","Next event","下一個行程");
        add("tapOpen","Zum Dashboard öffnen","Open dashboard","開啟資訊中心");
        add("cloud","Bewölkt","Cloudy","多雲");
        add("clear","Klar","Clear","晴朗");
        add("partcloud","Teilweise bewölkt","Partly cloudy","局部多雲");
        add("rainy","Regen","Rain","下雨");
        add("snowy","Schnee","Snow","下雪");
        add("foggy","Nebel","Fog","起霧");
        add("thunder","Gewitter","Thunderstorms","雷雨");
    }
    static String lang(Context c) { return Prefs.get(c).getString("lang","de"); }
    static String t(Context c, String key) {
        String[] words=TERMS.get(key); if(words==null) return key;
        String lang=lang(c); return words["en".equals(lang)?1:("zh-TW".equals(lang)?2:0)];
    }
    static String cond(Context c, int code) {
        if(code==0||code==1) return t(c,"clear");
        if(code==2) return t(c,"partcloud");
        if(code==3) return t(c,"cloud");
        if(code==45||code==48) return t(c,"foggy");
        if((code>=51&&code<=67)||(code>=80&&code<=82)) return t(c,"rainy");
        if((code>=71&&code<=77)||code==85||code==86) return t(c,"snowy");
        if(code>=95) return t(c,"thunder");
        return t(c,"weather");
    }
}
