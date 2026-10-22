# Netomax

Overlay Android pentru șoferi Bolt și Uber: citește oferta de pe ecran și arată profitul net calculat din profilul tău. Doar afișează. Nu apasă, nu acceptă și nu refuză nimic.

## Cum obții APK-ul
Nu există Gradle wrapper în arhivă și nici SDK Android în mediul unde a fost scris codul, deci **proiectul nu a fost compilat încă**.
1. Pune folderul pe GitHub (repo privat).
2. Fila Actions, workflow-ul „Build APK”, apoi descarcă artefactul `Netomax-debug-apk`.
3. Alternativ, local: Android Studio (Ladybug sau mai nou) deschide proiectul și Run. Sau `gradle assembleDebug` cu JDK 17 și Android SDK 35.

Dacă apar erori de compilare la prima rulare, sunt probabil mici (importuri, API-uri Compose); trimite-le și le repar.

## Activare
1. Instalează APK-ul și parcurge instalarea ghidată.
2. Setări, Accesibilitate, Netomax, pornește.
3. Android 13+, aplicație instalată din afara Play: dacă setarea e gri, Detalii aplicație, ⋮, „Permite setări restricționate”.

## Calibrare (obligatorie o dată pe telefon)
Pachetele (`ee.mtakso.driver`, `com.ubercab.driver`) și textele ecranelor nu sunt verificate pe un telefon real. Deschide Setări, Calibrare: vezi ce text citește aplicația, ce ofertă a înțeles și marchezi textul care apare doar pe ecranul „se caută curse”.

## Confidențialitate
Manifestul nu cere nicio permisiune, nici INTERNET. Datele stau în telefon (400 de zile, ca să vezi și anul) și se șterg din Setări.

## Limite cunoscute
- Bolt și Uber nu au API public. Citirea e euristică, iar oferte ambigue sunt marcate „neclare”, nu ghicite.
- Acceptarea cursei nu se poate detecta; cursele făcute se marchează manual în fila Curse.
- Consumul e în l/100 km peste tot (Marea Britanie cu mpg nu e acoperită).
- Google Play are reguli stricte pentru accesibilitate; distribuția prin APK direct e mai sigură.
- Verdictul: 70 la prag, 100 la dublu. Cost/km = consum × preț + (costuri fixe + uzură) / km pe an.

## Teste
`ro.profitride.core` (calcul, parser, statistici) are 36 de teste unitare, rulate local cu succes.

## Ce conține aplicația
Acasă (profit azi, inel de obiectiv zilnic), Curse (filtre Bolt/Uber/Acceptate, detaliu cu defalcarea costurilor, editare dacă citirea a greșit), Analiză (azi/săptămână/lună/an, încasări față de cheltuieli, venituri manuale), Costuri (mașină, costuri fixe detaliate, comisioane, comision de flotă, țintă zilnică), Setări (aspect zi/noapte/automat, overlay, calibrare).

## Oferte Bolt și Uber
Parserul a fost testat pe structura unei oferte Bolt reale (preț „9,82 lei (NET, taxe incluse)”, ridicare „6 min • 2.8 km”, cursă „7 min • 3.5 km”). Când ecranul spune „NET” sau „Net of Service Fee”, comisionul platformei nu se mai scade a doua oară. Poziția implicită a overlay-ului (16% de sus) cade între „Refuză” și cardul ofertei Bolt; o reglezi din Setări. Dacă nu apare nimic: Setări → Calibrare arată și „Ultima ofertă citită”, chiar după ce oferta a dispărut.

## Cele două moduri de citire
1. **Text (implicit):** când Bolt/Uber e deschis, serviciul citește textele ecranului. Funcționează și pe ecranul de start („Intră online”: apare „Gata · Intră online”), și pe „se caută curse” (radar), și pe oferte.
2. **Vizual (rezervă):** dacă ecranul nu dă text, aplicația face o captură (prin serviciul de accesibilitate, fără alte permisiuni), o transformă în text pe telefon cu ML Kit (modelul e inclus, fără internet) și apoi o citește la fel. Captura nu se salvează. Cere Android 11+. Se alege din Setări → „Cum citesc ecranul” (Automat / Text / Vizual). Consumă mai multă baterie decât textul, deci Automat o folosește doar când textul lipsește.
Nu se poate citi nimic dacă aplicația Bolt nu este deschisă pe ecran.

## Test cu o poză (Setări → „Test cu o poză")
- „Alege o poză din galerie": citește poza direct în aplicație (OCR pe telefon) și arată verdictul. Nu se salvează nimic.
- „Pornește testul (5 min)": apoi deschizi poza în galerie; suprapunerea apare ca la o ofertă reală (Android 11+, serviciul de accesibilitate pornit).
- Dacă nu citește: Setări → Calibrare → „Ultima ofertă citită".

## Pornit / Oprit
- Acasă: card „Netomax este pornit/oprit" cu buton Oprește/Pornește (apare când serviciul de accesibilitate e activ).
- Notificare fixă cu butonul Oprește/Pornește (Android 13+ cere permisiunea de notificări, cerută la finalul configurării).
- Ultimul pas al configurării: „Totul e gata" → „Pornește Netomax" sau „Mai târziu".
- Pe overlay nu există butoane (nu poate acoperi „Acceptă/Refuză").

## Poze și mașini
- Pasul „Mașina": marca, modelul și anul (2000 până azi) se aleg din liste; „Altul…" permite scris liber.
- Deschizi în galerie o poză cu o ofertă Bolt și overlay-ul apare singur (citire vizuală, doar în aplicații de galerie/foto; nu se salvează în istoric).

## Închidere și ecranul „online, fără ofertă"
- Aruncarea aplicației din lista de recente o oprește complet (overlay și notificare dispar). Rămâne oprită până apeși „Pornește”. Butonul „Înapoi" din ecranul principal doar o ascunde.
- Ecranul Bolt cu butonul „Deconectează-te" (sau „Go offline") se recunoaște ca „online, fără ofertă" și arată pulsul „Aștept cursa", și în aplicație, și într-o poză.
- Sub „Caut curse…” se rotesc mesaje clare: pragul („Ofertă bună: minim X”), câștigul de azi, cât mai ai până la obiectiv, cele mai bune ore.

## Meniul Auto
- Fila „Auto": tutorial la prima intrare (se poate reciti din Setări), documente cu dată de expirare (RCA, ITP, rovinietă, CASCO, copie conformă, permis, atestat), întreținere după kilometri, cheltuieli, număr și kilometraj.
- Datele se scriu ușor: datele doar cu cifre (12082026 devine 12.08.2026, cu corectare și sugestie la date imposibile), kilometri cu punct de mii automat, textele încep cu literă mare.
- Notificări: verificare zilnică în jur de 09:00 (și la deschiderea aplicației) cu 30, 10, 3 zile și în ziua expirării; la întreținere, când mai ai sub 1.000 km și când ai depășit.
- Costul de la RCA și CASCO se trece și în Costuri fixe (pe an). Cheltuielile din Auto sunt doar pentru evidență și nu se scad din profit, ca să nu se numere de două ori cu costurile din Costuri.

## Legal și date (GDPR)
- **Prima deschidere:** ecran de acord (Termeni, Confidențialitate, 18+). După „Accept și continuă” urmează setup-ul obișnuit. „Nu accept” duce la un ecran cu „Ieși din aplicație” (închide complet) și „Mă răzgândesc, revin” (înapoi la acord).
- **Setări → Legal și date:** Termeni (14 secțiuni), Confidențialitate (12), dezvăluire accesibilitate, **Datele mele** (export JSON, ștergere istoric, ștergere totală, retragerea acordului) și Despre.
- Dezvăluirea de accesibilitate apare ca dialog înainte de orice trimitere spre setările Android de accesibilitate.
- Fără acord (sau după retragere), serviciul de accesibilitate nu citește nimic.
- Acordul se salvează local (versiune + dată). Dacă mărești `ConsentStore.CURRENT_VERSION`, toți sunt întrebați din nou.

### De completat ÎNAINTE de publicare
Editează `app/src/main/res/values/legal_config.xml`: `legal_operator`, `legal_id`, `legal_address`, `legal_email` (acum sunt locuri goale în paranteze pătrate) și, dacă schimbi textele, `legal_terms_version` / `legal_effective`.
Textele trebuie verificate de un avocat. Google Play cere și o pagină web publică cu politica de confidențialitate.

## Citire pentru UK (în lucru)
- Parserul citește £, „mi”, „min/mins”, distanța în paranteze („5 mins (1.3 mi) away”) și ignoră „Toll Fee” ca preț.
- Uber UK pe Android poate arăta „1.5 mi trip” fără durată: durata cursei se estimează din viteza de la ridicare (sau 30 km/h). Câmpul `tripMinEstimated` marchează asta; overlay-ul nu îl afișează încă.
- Ecranele „Confirm Price”, „Rate passenger” și din timpul cursei nu sunt tratate ca oferte.
- Testat doar pe text reconstruit din screenshot-uri, nu pe un telefon din UK.
