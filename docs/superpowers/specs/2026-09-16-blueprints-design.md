# Elekto Blocks – Blueprint-System Design

Status: Entwurf zur Freigabe
Datum: 2026-09-16
Zielbranch: `android/blockly-modern-poc`
Ausgangsbasis: Hybrid 14

## Ziel

Elekto Blocks soll beliebige eigene Blockkombinationen lokal als **Blueprints** speichern und später wieder in die Arbeitsfläche einsetzen können.

Ein Blueprint darf aus einem einzelnen verbundenen Block-Stapel oder aus mehreren voneinander getrennten Block-Gruppen bestehen. Die räumliche Anordnung der ausgewählten Gruppen zueinander wird erhalten.

Das System bleibt vollständig lokal und offline. Blueprints sind App-Daten des Benutzers und keine fest eingebauten Blockly-Blöcke.

## Bedienmodell

### 1. Blueprint-Auswahl starten

Der bestehende lange Druck auf einen Blockly-Block öffnet weiterhin das native Menü **Block-Optionen**.

Dort kommt die neue Aktion **„Als Blueprint speichern“** hinzu.

Nach Auswahl dieser Aktion:

- schließt sich das Kontextmenü;
- der Editor wechselt in einen eigenen **Blueprint-Auswahlmodus**;
- der zusammenhängende Block-Verbund des ursprünglich gewählten Blocks ist bereits markiert;
- der Editor zeigt deutlich an, dass jetzt Gruppen für einen Blueprint ausgewählt werden.

### 2. Auswahlprinzip

Ein Tipp auf irgendeinen Block wählt immer den **gesamten zusammenhängenden Block-Verbund** aus.

Dabei gilt:

- Der Benutzer muss nicht den obersten Block treffen.
- Ein Tipp auf einen Block eines noch nicht ausgewählten Verbunds markiert den gesamten Verbund.
- Ein Tipp auf einen Block eines bereits ausgewählten Verbunds entfernt den gesamten Verbund wieder aus der Auswahl.
- Shadow-Blöcke und eingebettete Werte werden nicht separat ausgewählt; sie gehören zu ihrem übergeordneten Verbund.
- Mehrere voneinander getrennte Verbünde können gleichzeitig gewählt werden.
- Mindestens ein Verbund muss ausgewählt bleiben, damit ein Blueprint erstellt werden kann.

Technisch entspricht ein auswählbarer Verbund dem Root-Block und allen über Blockly-Verbindungen dazugehörigen Nachfolgern, Eingabeblöcken, Statement-Blöcken und Shadow-Blöcken.

### 3. Verhalten während des Auswahlmodus

Der Blueprint-Auswahlmodus ist bewusst kein normaler Bearbeitungsmodus.

Während er aktiv ist:

- Antippen dient ausschließlich zum Auswählen oder Abwählen von Verbünden.
- Blöcke werden nicht verschoben.
- Dropdowns und Werte werden nicht verändert.
- Das normale Long-Press-Kontextmenü ist deaktiviert.
- Die normale Lösch-Zone wird nicht angezeigt.

Ausgewählte Verbünde erhalten eine gut sichtbare zusätzliche Hervorhebung. Die Hervorhebung ist rein visuell und verändert keine Blockly-Daten.

Die native Oberfläche zeigt eine feste Aktionsleiste mit:

- **Abbrechen**
- einer kurzen Auswahl-Anzeige, beispielsweise `2 Gruppen ausgewählt`
- **Blueprint erstellen**

`Blueprint erstellen` ist nur aktiv, wenn mindestens ein Verbund ausgewählt ist.

### 4. Namen vergeben

Nach **Blueprint erstellen** erscheint ein natives Dialogfenster.

Der Benutzer gibt einen Namen ein, zum Beispiel:

- `LED blinken`
- `Potentiometer schaltet LED`
- `Motor 5-mal starten`

Regeln:

- Ein Name darf nicht leer sein.
- Leerzeichen am Anfang und Ende werden entfernt.
- Blueprints besitzen intern eine eindeutige ID; gleiche Anzeigenamen sind technisch erlaubt.
- Abbrechen verwirft nur den Speichervorgang und verändert die Arbeitsfläche nicht.

Nach erfolgreichem Speichern endet der Auswahlmodus und der normale Editor wird wieder aktiv.

## Block-Browser

Das bestehende Fenster **„Blöcke auswählen“** erhält zwei klar getrennte Hauptbereiche:

- **Blöcke**
- **Blueprints**

Diese Hauptauswahl steht über den bisherigen Block-Kategorien. `Blueprints` wird ausdrücklich **nicht** als weiterer Kategorie-Chip neben Grundlagen, Logik usw. eingefügt.

### Bereich „Blöcke“

Hybrid 14 bleibt zunächst funktional unverändert. Die heutige horizontale Kategorie-Leiste wird in einem späteren eigenen Schritt durch eine skalierbarere mobile Kategorienavigation ersetzt.

Diese spätere Navigation soll:

- eine Suche erhalten;
- Hauptkategorien kompakt auswählbar machen;
- bei Bedarf genau eine Unterkategorie-Ebene erlauben;
- keine immer länger werdende horizontale Chip-Leiste benötigen.

Die Browser-Umgestaltung ist nicht Teil der ersten Blueprint-Implementierung.

### Bereich „Blueprints“

Der Bereich zeigt die lokal gespeicherten Blueprints als Karten.

Eine Karte enthält in der ersten Version:

- Blueprint-Name;
- Anzahl der gespeicherten Hauptgruppen;
- Gesamtzahl der enthaltenen Blöcke;
- Aktion **Einfügen**.

Zusätzlich braucht jeder Blueprint eine einfache Verwaltung mit:

- **Umbenennen**
- **Löschen**

Eine grafische Mehrblock-Vorschau ist für die erste Version nicht erforderlich und kann später ergänzt werden.

Wenn noch keine Blueprints vorhanden sind, erklärt eine leere Ansicht kurz, wie der erste Blueprint erstellt wird.

## Einfügen eines Blueprints

Beim Antippen eines Blueprints im Browser wird er als neue Kopie in den aktuellen Workspace eingefügt.

Dabei gilt:

- Der gespeicherte Blueprint bleibt unverändert.
- Alle enthaltenen Gruppen werden neu erzeugt und erhalten neue Blockly-IDs.
- Die Verbindungen innerhalb jeder Gruppe bleiben erhalten.
- Die relative Position der getrennten Gruppen zueinander bleibt erhalten.
- Die gesamte Blueprint-Anordnung wird als Einheit in einen sinnvollen freien Bereich der aktuell sichtbaren Arbeitsfläche versetzt.
- Das Einfügen ist eine zusammengehörige Undo-Aktion, soweit Blockly dies unterstützt.

Blueprints sind Vorlagen, keine Verknüpfungen. Eine spätere Änderung des eingefügten Blocks verändert den gespeicherten Blueprint nicht.

## Datenmodell

Die native App besitzt die persistente Blueprint-Sammlung. Blockly liefert und verarbeitet nur den engine-spezifischen Block-Inhalt.

Ein Datensatz verwendet ein versioniertes Format, sinngemäß:

```json
{
  "schemaVersion": 1,
  "id": "uuid",
  "name": "LED blinken",
  "createdAt": 1789540000000,
  "updatedAt": 1789540000000,
  "groupCount": 2,
  "blockCount": 7,
  "engine": "blockly",
  "engineVersion": "13.3.0",
  "payload": {
    "groups": [
      {
        "x": 0,
        "y": 0,
        "blockState": {}
      },
      {
        "x": 240,
        "y": 80,
        "blockState": {}
      }
    ]
  }
}
```

Die tatsächlichen `blockState`-Objekte stammen aus Blockly Serialization.

### Positionsnormalisierung

Beim Speichern werden die Positionen der ausgewählten Root-Gruppen relativ zueinander gespeichert.

Der kleinste X- und Y-Wert der Auswahl wird als gemeinsamer Ursprung verwendet. Dadurch enthält der Blueprint keine absolute Position aus dem ursprünglichen Workspace.

Beim Einfügen wird dieser normalisierte Verbund an die aktuelle Einfügeposition verschoben.

## Persistenz

Die Blueprints werden ausschließlich im internen App-Speicher abgelegt.

Für die erste Version wird eine eigene native `BlueprintStore`-Schicht verwendet, die die Blueprint-Liste als versionierte JSON-Datei im internen App-Verzeichnis speichert.

Gründe:

- vollständig offline;
- kein Konto und kein Server;
- keine Abhängigkeit von WebView-`localStorage`;
- Blueprint-Daten bleiben App-Daten und nicht Blockly-UI-Daten;
- später leichter exportierbar, importierbar oder migrierbar.

Die WebView/Blockly-Schicht bekommt keine direkte Dateisystem-Verantwortung.

## Architektur und Schnittstellen

Die bestehende Trennung bleibt erhalten:

### Native Android UI

Verantwortlich für:

- `Blöcke | Blueprints`-Navigation;
- Auswahlmodus-Leiste;
- Namensdialog;
- Blueprint-Liste;
- Umbenennen und Löschen;
- persistente Speicherung über `BlueprintStore`.

### Elekto Editor Adapter

Erhält semantische Funktionen für Blueprints, beispielsweise:

- `startBlueprintSelection(blockId)`
- `finishBlueprintSelection()`
- `cancelBlueprintSelection()`
- `insertBlueprint(payload)`

Zusätzlich meldet die Editor-Seite Auswahländerungen und den fertigen serialisierten Payload an Android.

### Blockly Engine

Verantwortlich für:

- Ermitteln des Root-Verbunds eines angetippten Blocks;
- visuelle Mehrfachmarkierung;
- Serialisierung der gewählten Gruppen;
- Wiederherstellung der Gruppen;
- Erzeugen neuer Blockly-IDs;
- interne Verbindungen und Shadow-Blöcke.

Android manipuliert weiterhin keine Blockly-SVG-Verbindungen oder Blockly-Connection-Objekte direkt.

## Auswahl-Hervorhebung

Blockly besitzt standardmäßig nur eine normale Block-Selektion. Deshalb bekommt der Blueprint-Modus eine eigene Mehrfachmarkierung.

Die Markierung wird ausschließlich über temporäre CSS-Klassen beziehungsweise visuelle SVG-Styles auf die zu einem ausgewählten Root-Verbund gehörenden Block-SVGs angewendet.

Beim Beenden oder Abbrechen des Modus werden alle temporären Markierungen entfernt.

Diese Markierung darf weder Serialisierung noch Codegenerierung verändern.

## Fehlerfälle

Das System behandelt mindestens folgende Fälle kontrolliert:

- der ursprünglich lang gedrückte Block existiert beim Start nicht mehr;
- eine ausgewählte Gruppe wurde unerwartet entfernt;
- Serialisierung liefert keinen gültigen Payload;
- gespeicherte Blueprint-Datei ist leer oder beschädigt;
- ein Blueprint verwendet künftig eine nicht mehr unterstützte Schema-Version;
- Einfügen eines Blueprints schlägt teilweise fehl.

Bei einem Fehler darf kein halber Blueprint dauerhaft gespeichert werden. Beim Einfügen soll entweder der vollständige Blueprint entstehen oder die während des Vorgangs neu erzeugten Blöcke werden wieder entfernt.

## Tests

Die Implementierung folgt dem bestehenden Test-first-Ansatz aus Hybrid 14.

Automatisierte Tests decken mindestens ab:

1. Ein Tipp innerhalb eines verbundenen Stapels löst immer denselben Root-Verbund auf.
2. Auswahl eines zweiten getrennten Verbunds ergänzt die Auswahl.
3. Erneuter Tipp entfernt genau diesen Verbund.
4. Shadow-Blöcke werden nicht als eigene Gruppe behandelt.
5. Serialisierung mehrerer Gruppen erhält interne Verbindungen.
6. Relative Gruppenpositionen werden normalisiert und erhalten.
7. Ein eingefügter Blueprint erzeugt neue Block-IDs.
8. Löschen des Originals nach dem Speichern verändert den Blueprint nicht.
9. Umbenennen verändert nur Metadaten, nicht den Payload.
10. Löschen eines Blueprints entfernt ihn aus der persistenten Sammlung.
11. Abbrechen des Auswahlmodus verändert den Workspace nicht.
12. Die bestehende Block-Kontextfunktion aus Hybrid 14 bleibt außerhalb des Blueprint-Modus funktionsfähig.

Der Android-CI-Build darf erst nach erfolgreichen Blueprint-/Block-Action-Tests fortfahren.

## Bewusst nicht Teil der ersten Version

Noch nicht Bestandteil dieses Schritts sind:

- Cloud-Synchronisation;
- Benutzerkonten;
- öffentliche Blueprint-Bibliothek;
- Teilen zwischen Geräten;
- Export/Import als Datei;
- Favoriten;
- Blueprint-Ordner;
- automatische Vorschaubilder;
- Suchfunktion innerhalb der Blueprints;
- die vollständige Neugestaltung der Block-Kategorienavigation.

Diese Punkte bleiben möglich, ohne das Blueprint-Datenmodell grundsätzlich neu aufzubauen.

## Erfolgskriterien

Die erste Blueprint-Version ist erfolgreich, wenn der Benutzer auf dem Handy:

1. einen beliebigen Block lange drücken kann;
2. **Als Blueprint speichern** wählen kann;
3. automatisch den gesamten dazugehörigen Stapel markiert sieht;
4. durch einfaches Antippen weitere getrennte Stapel hinzufügen oder entfernen kann;
5. den Blueprint benennen und lokal speichern kann;
6. ihn unter **Blöcke → Blueprints** wiederfindet;
7. ihn mit allen Gruppen und ihrer relativen Anordnung erneut einfügen kann;
8. ihn umbenennen oder löschen kann;
9. danach normal weiterarbeiten und Undo/Redo verwenden kann.
