package com.nimbus.weather;

import java.util.HashMap;
import java.util.Map;

final class UI {
  private static final Map<String,String[]> V=new HashMap<>();
  static {
    add("home","Start","Home","首頁");
    add("planner","Kalender","Planner","行事曆");
    add("explore","Verkehr","Transport","交通");
    add("more","Mehr","More","更多");
    add("hello","Guten Morgen","Good morning","早安");
    add("afternoon","Guten Tag","Good afternoon","午安");
    add("evening","Guten Abend","Good evening","晚安");
    add("overview","Alles für deinen Tag.","Everything for your day.","今天需要知道的一切。");
    add("today","HEUTIGER PLAN","TODAY'S PLAN","今日行程");
    add("date","Datum","Date","日期");
    add("next","ALS NÄCHSTES","UP NEXT","接下來");
    add("bring","HEUTE MITNEHMEN","BRING TODAY","今天帶什麼");
    add("weather","WETTER","WEATHER","天氣");
    add("depart","Zuhause los","Leave home","從家裡出發");
    add("arrive","Ankunft","Arrive","抵達");
    add("walk","Fußweg","Walk","步行");
    add("route","Route anzeigen","See route","查看路線");
    add("forecast","Wetterdetails","Weather details","天氣詳情");
    add("sunrise","Sonnenaufgang","Sunrise","日出");
    add("sunset","Sonnenuntergang","Sunset","日落");
    add("high","Höchst","High","最高");
    add("low","Tiefst","Low","最低");
    add("rain","Regen","Rain","降雨");
    add("uv","UV","UV","紫外線");
    add("wind","Wind","Wind","風速");
    add("feels","Gefühlt","Feels like","體感");
    add("refresh","Aktualisieren","Refresh","更新");
    add("updated","Aktualisiert","Updated","更新於");
    add("permissionTitle","Zugriff erlauben","Allow access","允許存取");
    add("permissionWhy","Mit Standort und Kalender kann Nimbus deine lokale Wettervorhersage und heutigen Termine anzeigen. Beide Berechtigungen sind optional und können später in den Android-Einstellungen geändert werden.","Location and calendar access let Nimbus show local weather and your agenda. Both are optional; you can change them later in Android settings.","允許位置與行事曆權限後，Nimbus 才能顯示所在地天氣與行程。兩者皆可選擇不授權，之後也能在 Android 設定中修改。");
    add("permissions","Standort- und Kalenderzugriff","Location and calendar access","位置與行事曆權限");
    add("manualMode","Standort fixieren","Lock weather to a city","固定天氣城市");
    add("autoMode","Automatisch: aktueller Standort","Automatic: current location","自動：目前位置");
    add("manualCity","Fixierter Ort (nur bei aktivierter Fixierung)","Locked city (only when enabled)","固定城市（開啟後使用）");
    add("locationDenied","Standort nicht erlaubt. Fixiere eine Stadt, um Wetter anzuzeigen.","Location access denied. Lock a city for weather.","未授權位置；請固定城市以查看天氣。");
    add("locationWait","Standort wird ermittelt …","Finding your location…","正在定位……");
    add("locationUnavailable","Standort derzeit nicht verfügbar. Bitte erneut versuchen.","Location unavailable. Please try again.","目前無法定位，請稍後再試。");
    add("yourLocation","Dein Standort","Your location","你的位置");
    add("onlyVbb","Live-Verbindungen derzeit nur in Berlin/Brandenburg. Für andere Städte Google Maps verwenden.","Live transit is currently available only in Berlin/Brandenburg. Use Google Maps elsewhere.","即時交通目前僅支援柏林／布蘭登堡。其他城市請用 Google 地圖。");
    add("noEvents","Heute keine Kalendereinträge. Kalenderzugriff prüfen oder Termin erstellen.","No events today. Check calendar permission or create one.","今天沒有行程；請檢查行事曆權限或新增行程。");
    add("noUpcoming","Kein anstehender Termin mit Fahrtroute.","No upcoming event with a route.","目前沒有需要規劃交通的活動。");
    add("noRoute","Keine geprüfte Route verfügbar. Route in Google Maps öffnen.","No verified route available. Open directions in Google Maps.","目前沒有已確認的路線；請使用 Google 地圖。");
    add("nextEvent","Nächster Termin","Next event","下一個行程");
    add("allDay","Ganztägig","All day","全天");
    add("done","Erledigt","Packed","已準備");
    add("things","Packliste","Packing list","攜帶清單");
    add("plannerDesc","Termine aus deinem Android-Kalender, auch von synchronisierten Google- und Samsung-Kalendern.","Events on your device, including synced Google and Samsung calendars.","顯示手機行事曆中的活動，包括已同步的 Google 與 Samsung 行事曆。");
    add("editCalendar","Kalender öffnen","Open calendar","開啟行事曆");
    add("newEvent","Neuer Termin","New event","新增行程");
    add("nearby","Nächste Abfahrten","Next departures","即將發車");
    add("maps","In Google Maps öffnen","Open in Google Maps","使用 Google 地圖");
    add("settings","MEINE EINSTELLUNGEN","MY SETTINGS","我的設定");
    add("profile","Vorname (optional)","First name (optional)","名字（選填）");
    add("city","Stadt","City","城市");
    add("station","Starthaltestelle (optional)","Origin stop (optional)","出發車站（選填）");
    add("homeAddr","Zuhause (nur auf deinem Gerät)","Home (device only)","住家地址（僅存於手機）");
    add("dest","Uni oder Standardziel","University or default destination","大學或預設目的地");
    add("activity","Weitere Pläne / Aktivitäten","Other plans / activities","其他計畫／活動");
    add("walkBuffer","Minuten von Zuhause zur Haltestelle","Minutes from home to stop","住家走到車站的分鐘數");
    add("language","Sprache","Language","語言");
    add("fahrenheit","Fahrenheit (°F)","Fahrenheit (°F)","華氏 (°F)");
    add("save","Änderungen speichern","Save changes","儲存變更");
    add("saving","Speichere Einstellungen …","Saving settings…","正在儲存設定……");
    add("saved","Gespeichert","Saved","已儲存");
    add("cityError","Stadt nicht gefunden; vorherige Auswahl bleibt.","City not found; previous selection remains.","找不到城市，保留之前的設定。");
    add("chat","MIT CHATGPT PLANEN","PLAN WITH CHATGPT","透過 CHATGPT 規劃");
    add("chatDetails","Aktuelle Tagesdaten kopieren und ChatGPT öffnen. ChatGPT kann die App nicht selbstständig im Hintergrund steuern.","Copy today's context and open ChatGPT. ChatGPT cannot control the app in the background.","複製今天的資料並開啟 ChatGPT；ChatGPT 無法自行在背景控制 App。");
    add("askChat","Tagesplanung mit ChatGPT","Plan my day with ChatGPT","用 ChatGPT 規劃今天");
    add("copyDone","Tagesdaten kopiert. In ChatGPT einfügen und absenden.","Daily data copied. Paste and send in ChatGPT.","已複製今日資料，請貼到 ChatGPT 並送出。");
    add("import","Update aus ChatGPT einfügen","Paste update from ChatGPT","貼上 ChatGPT 提供的更新");
    add("importHelp","NIMBUS_V1-JSON aus dem Chat kopieren. Änderungen werden vor dem Speichern bestätigt. Keine automatische Synchronisierung.","Copy NIMBUS_V1 JSON from chat. Review before saving. No automatic synchronization.","從對話複製 NIMBUS_V1 JSON，確認後才儲存；不會自動同步。");
    add("importEmpty","Keine gültigen Nimbus-Einstellungen in der Zwischenablage.","No valid Nimbus settings on the clipboard.","剪貼簿沒有有效的 Nimbus 設定。");
    add("importReview","Diese Angaben übernehmen?","Apply these settings?","要套用這些設定嗎？");
    add("cancel","Abbrechen","Cancel","取消");
    add("apply","Übernehmen","Apply","套用");
    add("dataLocal","Private Adressen bleiben lokal auf deinem Handy; kein Backend und keine automatische ChatGPT-Synchronisierung.","Private addresses stay on your phone; no backend or automatic ChatGPT sync.","私人地址僅存於手機；沒有伺服器或自動 ChatGPT 同步。");
    add("loading","Aktuelle Daten werden geladen …","Loading current information…","正在更新資訊……");
    add("error","Informationen teils nicht verfügbar. Prüfe Internet und Berechtigungen.","Some information unavailable. Check connectivity and permissions.","部分資訊無法取得，請檢查網路與權限。");
    add("source","Wetter: Open-Meteo · Verkehr: VBB · Kalender: Android","Weather: Open-Meteo · Transit: VBB · Calendar: Android","天氣：Open-Meteo · 交通：VBB · 行事曆：Android");
    add("expired","Letzter bekannter Standort","Last known location","最後已知位置");
  }
  private static void add(String k,String de,String en,String zh){V.put(k,new String[]{de,en,zh});}
  static String t(String l,String k){String[] a=V.get(k);return a==null?k:a["en".equals(l)?1:"zh".equals(l)?2:0];}
  private UI(){}
}
