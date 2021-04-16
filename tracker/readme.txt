RPI = Rolling Proximity Identifiers
-> werden alle ~10 min gewechselt (also 144 mal ab 0 Uhr an)
-> RPI = f(TEK, #)

TEK = Temporary Exposure Key
-> 1 Tag gültig
-> wird nicht rausgegeben (außer nach Zustimmung zum teilen bei positivem Test)
-> wird veröffentlicht von Infizierten (14 TEKs, 1 TEK pro Tag)

GUID = Global Unique Idetifier

RT = Registration Token
-> wird mit hash(GUID) generiert
-> wird benötigt um Daten zu bekommen

TAN = Transaktions Access Number

RSIN = Rolling Start Interval Number

ENIN = Exposure Notification Interval Number


CWA Backend Server Communication:
-> UUID generierung mit sha 256 hash
    -> Token für verification Server
    -> Post Request registrationToken {key, keyType:GUID}
    -> Rückgabe: Registration Token

-> mit dem Registration Token (RT) können Testergebnisse vom Verification Server abgeholt werden
-> nach Positiven Test (testResult:"2") muss der RT beim Verification Server eine TAN erstellen
-> nur mit der TAN können die TEKs in den CWA Server hochgeladen werden
    -> TEKs als Binärdatei (oder auch Diagnosis Keys)
    -> TAN wird mit Verification Server abgeglichen

-> Aus Keys und Rolling Update Periode können die TEKs für den Tag errechnet werden

Quellen:

Funktionsweise:
https://www.youtube.com/watch?v=MEQ0wzk1Cp8&ab_channel=predic8

Softwarearchitektur:
https://www.youtube.com/watch?v=ytglSxeTPyU&ab_channel=predic8

Backend Funktionalität 1:
https://www.youtube.com/watch?v=RKoBcsCA5ts&ab_channel=predic8

Backend Funktionalität 2:
https://www.youtube.com/watch?v=7mebVNWcGxU&ab_channel=predic8