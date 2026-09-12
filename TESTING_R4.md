# UNO R4 WiFi – erster Funktionstest

Diese Anleitung gilt fuer den Entwicklungsbranch `feature/uno-r4-wifi`.

## Start unter Windows – ohne Python

1. Den Branch `feature/uno-r4-wifi` als ZIP herunterladen und entpacken.
2. Im entpackten Hauptordner `START_R4.cmd` doppelklicken.
3. Ein PowerShell-Fenster startet den lokalen Server auf `127.0.0.1:8000`.
4. Der Standardbrowser oeffnet automatisch `http://127.0.0.1:8000/r4.html`.
5. Das PowerShell-Fenster waehrend des Tests offen lassen.
6. Zum Beenden im PowerShell-Fenster `Strg+C` druecken.

Es wird weder Python noch eine weitere Server-Software benoetigt.

## Test 1 – R4-Profil wird geladen

In Blockly@rduino muss bei der Board-Auswahl `Arduino UNO R4 WiFi` vorhanden sein. Die R4-Seite verwendet dieses Board standardmaessig.

Falls das Board nicht angezeigt wird, den Browser mit `Strg+F5` neu laden und pruefen, ob wirklich das neu heruntergeladene ZIP verwendet wird.

## Test 2 – Blink-Code erzeugen

Erzeuge mit Blockly ein einfaches Programm fuer D13:

- D13 HIGH
- 1000 ms warten
- D13 LOW
- 1000 ms warten
- wiederholen

Wechsle anschliessend zur Arduino-Codeansicht und kopiere den erzeugten Code.

Erwartet wird sinngemaess ein normaler Arduino-Sketch mit `pinMode(13, OUTPUT)`, `digitalWrite(13, HIGH/LOW)` und `delay(1000)`.

## Test 3 – auf echtem UNO R4 WiFi

1. Arduino IDE oeffnen.
2. Board `Arduino UNO R4 WiFi` auswaehlen.
3. Den von Blockly erzeugten Code einfuegen.
4. Kompilieren.
5. Auf das Board hochladen.
6. Pruefen, ob die LED an D13 im Sekundentakt blinkt.

Damit testen wir zunaechst bewusst nur:

`Blockly -> Arduino-Code -> Arduino IDE -> UNO R4 WiFi`

Der direkte Upload aus Blockly@rduino wird erst danach separat getestet.

## Testprotokoll

| Test | Status | Bemerkung |
|---|---|---|
| R4 WiFi in Board-Auswahl | offen | |
| Blink-Code wird erzeugt | offen | |
| Blink kompiliert in Arduino IDE | offen | |
| Blink laesst sich hochladen | offen | |
| LED D13 blinkt korrekt | offen | |
