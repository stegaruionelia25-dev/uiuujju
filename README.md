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
Manifestul nu cere nicio permisiune, nici INTERNET. Datele stau în telefon (90 de zile) și se șterg din Setări.

## Limite cunoscute
- Bolt și Uber nu au API public. Citirea e euristică, iar oferte ambigue sunt marcate „neclare”, nu ghicite.
- Acceptarea cursei nu se poate detecta; cursele făcute se marchează manual în fila Curse.
- Consumul e în l/100 km peste tot (Marea Britanie cu mpg nu e acoperită).
- Google Play are reguli stricte pentru accesibilitate; distribuția prin APK direct e mai sigură.
- Verdictul: 70 la prag, 100 la dublu. Cost/km = consum × preț + (costuri fixe + uzură) / km pe an.

## Teste
`ro.profitride.core` (calcul, parser, statistici) are 27 de teste unitare, rulate local cu succes.
