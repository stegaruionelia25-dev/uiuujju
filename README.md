# ProfitRide

Overlay Android pentru șoferi Bolt și Uber: citește oferta de pe ecran și arată profitul net calculat din profilul tău. Doar afișează. Nu apasă, nu acceptă și nu refuză nimic.

## Cum obții APK-ul
Nu există Gradle wrapper în arhivă și nici SDK Android în mediul unde a fost scris codul, deci **proiectul nu a fost compilat încă**.
1. Pune folderul pe GitHub (repo privat).
2. Fila Actions, workflow-ul „Build APK”, apoi descarcă artefactul `ProfitRide-debug-apk`.
3. Alternativ, local: Android Studio (Ladybug sau mai nou) deschide proiectul și Run. Sau `gradle assembleDebug` cu JDK 17 și Android SDK 35.

Dacă apar erori de compilare la prima rulare, sunt probabil mici (importuri, API-uri Compose); trimite-le și le repar.

## Activare
1. Instalează APK-ul și parcurge instalarea ghidată.
2. Setări, Accesibilitate, ProfitRide, pornește.
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
