# Prompt: Organi7er als native Android-App bauen

> Diesen gesamten Text als ersten Auftrag in den Chat geben. Die Designvorlage liegt im Repo unter `design/designsystem.html` und ist die verbindliche Referenz für Farben, Abstände und Aufbau.

---

## Auftrag

Baue **Organi7er**, eine native Android-App zum Verwalten von **Terminen, Aufgaben und Notizen**. Komplett in **Kotlin mit Jetpack Compose**, keine Cross-Platform-Frameworks, keine WebViews. Sprache der App: **Deutsch**.

Öffne zuerst `design/designsystem.html` im Browser (oder lies den Quelltext). Alles, was dort zu sehen ist, soll in Compose nachgebaut werden: Farb-Tokens, Typografie, Komponenten und die vier Bildschirme. Wenn diese Beschreibung und die HTML-Vorlage sich widersprechen, gilt die HTML-Vorlage.

Arbeite in den Phasen unten. Nach jeder Phase muss die App bauen und starten (`./gradlew assembleDebug`), Lint und Tests müssen grün sein. Committe in kleinen, sinnvollen Schritten mit deutschen Commit-Nachrichten. Pushe nur, wenn ich es ausdrücklich sage.

## Technik

- **Sprache/UI:** Kotlin, Jetpack Compose, Material 3 als Basis, aber mit eigenem Theme (siehe Tokens). Edge-to-Edge.
- **Architektur:** Single-Activity, MVVM mit `ViewModel` + `StateFlow`, Unidirectional Data Flow. Pakete nach Feature: `feature/overview`, `feature/calendar`, `feature/tasks`, `feature/notes`, `feature/settings`, dazu `core/designsystem`, `core/data`, `core/model`.
- **Navigation:** Navigation Compose mit typsicheren Routen.
- **Daten:** Room (lokal, offline-first), DataStore (Preferences) für Einstellungen.
- **DI:** Hilt.
- **Bilder:** Photo Picker (`PickVisualMedia`) und Kamera über `TakePicture`, Bilder im App-Speicher ablegen, Anzeige mit Coil.
- **Erinnerungen:** `AlarmManager` für exakte Termin-Erinnerungen, WorkManager für das tägliche Morgen-Briefing. Notification-Channels sauber anlegen, Berechtigung `POST_NOTIFICATIONS` erst bei Bedarf erfragen.
- **Glas-Effekt:** Hintergrund-Unschärfe mit der Bibliothek *Haze* (dev.chrisbanes.haze). Unter API 31 ohne Blur, nur halbtransparente Fläche.
- **Schrift:** Outfit (Google Fonts), Gewichte 300, 400, 500, 600 als Dateien unter `res/font`.
- **Build:** Gradle Kotlin DSL mit Version Catalog (`libs.versions.toml`), minSdk 26, aktuelles targetSdk. Verwende jeweils die **aktuelle stabile Version** jeder Bibliothek, prüfe das statt Versionen aus dem Gedächtnis zu nehmen.
- **Qualität:** ktlint oder detekt, Unit-Tests für ViewModels und Repository, ein paar Compose-UI-Tests für die Navigation. Alle Tippflächen mindestens 48 dp, `contentDescription` für Icon-Knöpfe, TalkBack muss sinnvoll vorlesen.

## Design-Tokens

Lege alles in `core/designsystem` an (`Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Theme.kt`). Im Code nur semantische Namen verwenden, nie Hex-Werte in Screens.

### Kernfarben (60 / 30 / 10)

| Rolle | Name | Hex |
|---|---|---|
| 60 % dominant | Deep Navy | `#061B3A` |
| 30 % unterstützend | Electric Blue | `#3B82F6` (gefüllte Knöpfe mit weißer Schrift: `#2563EB`) |
| 10 % Akzent | Bold Yellow | `#FFD500` (Schrift darauf immer Navy, nie Weiß) |

### Semantische Tokens

| Token | Hell | Dunkel | Einsatz |
|---|---|---|---|
| bg | `#F4F7FB` | `#03112A` | Bildschirmhintergrund |
| surface | `#FFFFFF` | `#061B3A` | Karten |
| surface2 | `#EEF2F8` | `#0F2A55` | Segment-Spur, Fortschritt-Spur |
| glass | Weiß 62 % | `#0F2A55` 55 % | Glas-Karten (mit Blur) |
| border | `#D7DCE5` | `#1A3665` | Linien |
| text | `#061B3A` | `#F4F7FB` | Haupttext |
| textMuted | `#4A5B78` | `#B3C2DB` | Sekundärtext |
| textSoft | `#5E6D88` | `#8399BE` | Meta, Platzhalter |
| primary / onPrimary | `#2563EB` / Weiß | `#2563EB` / Weiß | gefüllte Knöpfe, Auswahl |
| brandBlue | `#3B82F6` | `#60A5FA` | Icons, Fokus, Verläufe, Links (dunkel) |
| accent / onAccent | `#FFD500` / `#061B3A` | gleich | „Jetzt“-Linie, Fortschritt in Kacheln, Priorität |
| hero / onHero | `#061B3A` / Weiß | `#0F2A55` / Weiß | Karte „Als Nächstes“, gewählte Kachel (hell) |
| nav | `#061B3A` 86 % | `#0A2248` 82 % | Navigationsleiste (Glas) |
| typeEvent / on | `#2563EB` / Weiß | gleich | Add-Button Termin |
| typeTask / on | `#FFD500` / `#061B3A` | gleich | Add-Button Aufgabe |
| typeNote / on | `#061B3A` / Weiß | `#F4F7FB` / `#061B3A` | Add-Button Notiz |
| paperLine / paperMargin | `#DBE8FE` / `#93BBFD` | `#1A3665` / `#2E4C7E` | Linien im Notizblatt |
| calWork / calPrivate / calHealth | `#3B82F6` / `#FFD500` / `#22C55E` | gleich | Standard-Kalenderfarben |

Die vollständigen Farbskalen (navy-50 … navy-950, blue-50 … blue-900, yellow-50 … yellow-900) stehen in der HTML-Datei im `:root`-Block. Übernimm sie als Primitive.

Hell/Dunkel folgt dem System, in den Einstellungen überschreibbar (System / Hell / Dunkel).

### Typografie (Outfit)

| Stil | Gewicht | Größe/Zeile | Laufweite |
|---|---|---|---|
| Display („Heute“) | 300 | 46/48 sp | −4 % |
| Seitentitel („Termine“) | 300 | 30/34 sp | −3 % |
| Headline (Karte „Als Nächstes“) | 400 | 22/28 sp | −1 % |
| Title (Abschnitte) | 500 | 17/24 sp | −1 % |
| Body | 400 | 15/22 sp | 0 |
| Label | 500 | 13/18 sp | 0 |
| Caption | 400 | 12/16 sp | 0 |
| Overline | 500 | 12 sp, Versalien | +8 % |

Kein Fettdruck über 600. Ruhe entsteht durch leichte große Schrift.

### Form und Tiefe

- Radien: 10 (Notizblatt), 14, 20 (Zeilen, Bilder), 22 (Kacheln), 28 (Karten), 34 (große Karten), Pille.
- Abstände im 4er-Raster, Seitenrand 18 dp, Abschnittsabstand 22 dp.
- Zwei Schatten: *soft* (Karten, runde Knöpfe: y 8, blur 24, Navy 8 %) und *float* (Navigationsleiste, Add-Button: y 12, blur 32, Navy 18 %).
- Soft-UI-Knöpfe: runder Knopf mit leichtem vertikalem Verlauf (oben Weiß, unten `#E9EEF6`; dunkel `#14305C` → `#0A2248`) plus feiner heller Innenkante oben.

## Komponenten (in `core/designsystem/components`)

Nachbauen wie in der Vorlage, jeweils mit Preview in Hell und Dunkel:

1. **SoftIconButton** (rund, 40 und 48 dp)
2. **PillButton** in den Varianten Primary, Soft, Ghost, Dark sowie **TypeButton** (Termin/Aufgabe/Notiz in ihrer Typfarbe)
3. **ExtendedAddButton**: Höhe 54 dp, Radius 18, Plus-Icon + Text („Neuer Termin“, „Neue Aufgabe“, „Neue Notiz“), Typfarbe
4. **GlassCard** (mit Haze-Blur)
5. **BriefingTile**: Titel, drei Zähler mit Icons (Termine, Aufgaben, Notizen mit Erinnerung), unten nächste Uhrzeit mit Uhr-Icon. Der **gelbe Fortschrittsbalken** erscheint nur, wenn etwas in den nächsten 60 Minuten beginnt, er füllt sich bis zum Beginn. Gewählte Kachel: hell = Navy mit weißer Schrift, dunkel = `#F4F7FB` mit Navy-Schrift.
6. **NextEventCard** (große Navy-Karte mit blauem Lichtschein, Zeit, Titel, Ort, Dauer, Teilnehmer-Avatare, Pfeil-Knopf, gelber Chip „in 25 Min“)
7. **EventRow** (Uhrzeit, farbiger Balken in Kalenderfarbe, Titel, Ort)
8. **TaskRow** mit rundem Soft-Häkchen, Titel, Meta, gelbem Prioritätspunkt; erledigt = durchgestrichen und gedämpft
9. **DraftRow** (Titel, „vor 3 Tagen“, Knopf „Planen“)
10. **SegmentedControl** (Soft, gewähltes Segment hebt sich als heller Knopf ab)
11. **SearchField** (Pille, Glas)
12. **DateDivider**: Datum mittig, links und rechts eine dünne Wellenlinie
13. **NoteCard** im Logbuch-Muster: links Bilderstapel (oder gestrichelter Platzhalter „Foto aufnehmen / oder aus Galerie“), rechts ein kleines liniertes Notizblatt mit blauer Randlinie, leicht gedrehtem Blatt dahinter, Titel und Vorschau, Pille „2 Notizen“
14. **Switch** im Soft-Stil
15. **FloatingTabBar**: schwebende Navy-Glas-Pille, 4 Reiter. Aktiver Reiter = **weiße Pille** mit Icon und Text in Navy, die anderen nur Icon (mit `contentDescription`). Klappt beim Runterscrollen nach unten weg und kommt beim Hochscrollen zurück.

## Bildschirme

### Navigation
Vier Reiter: **Übersicht**, **Termine**, **Aufgaben**, **Notizen**. Es gibt **keinen** zentralen Add-Button in der Leiste.
- Auf der Übersicht gibt es keinen schwebenden Add-Button.
- Auf Termine, Aufgaben und Notizen sitzt rechts über der Leiste der **ExtendedAddButton** in der Typfarbe (wie Gmail). Beim Scrollen schrumpft er auf das Plus-Icon.
- Einstellungen sind ein eigener Bildschirm, erreichbar über das Zahnrad oben rechts in der Übersicht. Dort ist die Leiste ausgeblendet, oben links ein Zurück-Knopf.

### Übersicht
Von oben nach unten:
1. Kopfzeile: Wortmarke „Organi**7**er“ (die 7 mit gelbem Unterstrich) links, Zahnrad (Einstellungen) rechts.
2. „Heute“ groß (Display) und daneben das Kurzdatum, z. B. „Mo., 23. Nov.“ (aus dem Systemdatum, deutsch formatiert). **Keine** Begrüßung, **keine** Zähler-Chips.
3. **Drei Briefing-Kacheln** nebeneinander: Heute, Morgen, Woche. Sie funktionieren wie Reiter und steuern den Inhalt darunter.
4. **Drei Schnell-Knöpfe** in einer Reihe: „+ Termin“ (blau), „+ Aufgabe“ (gelb), „+ Notiz“ (Navy, dunkel hell).
5. Inhalt der gewählten Kachel:
   - *Heute:* „Als Nächstes“ mit NextEventCard und den weiteren Terminen, danach „Aufgaben heute“.
   - *Morgen:* „Termine morgen“, „Aufgaben morgen“.
   - *Woche:* Termine der Woche kompakt nach Tag, „Fällig diese Woche“.
6. Darunter immer der **Notizen-Feed** nach Tagen gruppiert (DateDivider + NoteCard).

### Termine
- Kopf: kleine Zeile „November 2026 ⌄“ (öffnet eine Monatsansicht zur Datumswahl), darunter Seitentitel „Termine“. Rechts ein Soft-Knopf „Heute“, der zum heutigen Tag springt.
- Wochenleiste **ohne** umgebenden Container: Wochentag klein, darunter Tageszahl im Kreis; gewählter Tag gefüllt (Navy, dunkel invertiert), heutiger Tag mit blauem Ring. Darunter Punkte in den Kalenderfarben des Tages. Wischen wechselt die Woche.
- Legende der Kalender (Arbeit, Privat, Gesundheit).
- Liste der Termine des Tages, jeder Termin in **seiner Kalenderfarbe** (getönter Hintergrund + farbiger linker Rand). Eine gelbe „Jetzt“-Linie mit Uhrzeit zwischen vergangenen und kommenden Terminen. Ein Stundenraster ist für später vorgesehen, jetzt noch nicht.
- Termin anlegen/bearbeiten als Bottom Sheet: Titel, Datum, Start/Ende oder ganztägig, Ort, Kalender (Farbe), Erinnerung, Notiz.

### Aufgaben
- Seitentitel nur „Aufgaben“ (kein Fortschrittsbalken, kein „2 von 7 erledigt“).
- Segment: **Entwürfe · Offen · Erledigt**, Standard ist *Offen*.
  - *Entwürfe:* schnell notierte Gedanken ohne Datum, kurzer Hinweistext oben. „Planen“ öffnet ein Sheet für Datum/Uhrzeit, danach wandert die Aufgabe nach *Offen*. Entwürfe sollen bewusst keinen Druck erzeugen.
  - *Offen:* gruppiert nach Überfällig, Heute, Diese Woche, Später.
  - *Erledigt:* die letzten 30 Tage, ältere einklappbar.
- Abhaken mit kurzer Animation und Rückgängig-Snackbar.
- Neue Aufgabe als Bottom Sheet: Titel, optional Datum/Uhrzeit, Priorität, Liste/Tag. Ohne Datum landet sie in Entwürfe.

### Notizen
- Seitentitel nur „Notizen“.
- Suchfeld, darunter drei Filterkacheln mit Zähler: Alle, Mit Bild, Nur Text (gewählte Kachel Navy).
- Feed nach Tagen gruppiert mit DateDivider und NoteCard (Logbuch-Muster). Sortier-Knopf oben rechts (neueste/älteste zuerst).
- Notiz-Detail: Titel, Text auf liniertem Papier (24 dp Zeilenabstand, Text sitzt auf den Linien), Bilder oben als horizontaler Stapel, Bild antippen öffnet Vollbild mit Zoom. Optionale Erinnerung pro Notiz.

### Einstellungen
Gruppen wie in der Vorlage: Darstellung (System/Hell/Dunkel), Übersicht (Kachel „Morgen“ zeigen, Woche beginnt am), Erinnerungen (Vorlauf vor Terminen, Morgen-Briefing mit Uhrzeit, Entwürfe nach 7 Tagen erinnern), Konten und Daten (Kalender und Farben verwalten, Sicherung als JSON-Export/Import), Versionsnummer.

## Datenmodell (Room)

- `Event`: id, title, start, end, allDay, location, calendarId, reminderMinutes, note, createdAt, updatedAt
- `CalendarEntity`: id, name, colorHex (Standard: Arbeit, Privat, Gesundheit)
- `Task`: id, title, note, dueAt (null = Entwurf), priority, status (DRAFT, OPEN, DONE), doneAt, listName, createdAt, updatedAt
- `Note`: id, title, body, reminderAt, createdAt, updatedAt
- `NoteImage`: id, noteId, filePath, position
- Zeitangaben als `Instant`/`LocalDateTime` über `java.time`, Zeitzone des Geräts.

Die Briefing-Zähler (Termine, Aufgaben, Notizen mit Erinnerung) und die „nächste Uhrzeit“ pro Zeitraum kommen aus einem eigenen `BriefingRepository`, das als `Flow` aktualisiert. Die „in X Min“-Anzeige aktualisiert sich jede Minute.

Lege beim ersten Start Beispieldaten an (wie in der HTML-Vorlage), mit Schalter in den Einstellungen zum Löschen.

## Phasen

1. **Projekt-Setup:** Gradle, Version Catalog, Hilt, Theme mit allen Tokens, Outfit-Schrift, Edge-to-Edge. Ein Katalog-Bildschirm (nur Debug) mit allen Komponenten in Hell und Dunkel.
2. **Navigation-Gerüst:** FloatingTabBar, vier leere Reiter, Einstellungen, ExtendedAddButton je Reiter.
3. **Datenschicht:** Room-Entities, DAOs, Repositories, DataStore, Beispieldaten, Tests.
4. **Übersicht** komplett inklusive Briefing-Kacheln und Notizen-Feed.
5. **Termine** mit Wochenleiste, Liste, Anlegen/Bearbeiten.
6. **Aufgaben** mit Entwürfe/Offen/Erledigt, Planen, Anlegen.
7. **Notizen** mit Feed, Filter, Suche, Detail, Bildern.
8. **Einstellungen und Erinnerungen** (Notifications, Morgen-Briefing).
9. **Feinschliff:** Animationen (dezent, 200–250 ms), Barrierefreiheit, große Schrift (bis 200 %), Leere-Zustände, Screenshots aller Bildschirme in Hell und Dunkel.

Nach jeder Phase: kurz zusammenfassen, was fertig ist, was offen ist, und Screenshots (Emulator, Pixel-Gerät) zeigen.

## Nicht machen

- Keine Login-/Cloud-Funktion in dieser Version.
- Keine Material-Standardfarben (kein Lila), keine Schatten aus der Material-Elevation, nur die zwei definierten.
- Keine weiße Schrift auf Gelb, kein reines Schwarz.
- Keine Begrüßungstexte oder Zähler-Chips auf der Übersicht.
