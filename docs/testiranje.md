# Testiranje softvera 2025/26

Za potrebe polaganja predispitnih obaveza na predmetu Testiranje softvera, neophodno je obezbediti testove zahteva aplikacije koji su opisani u nastavku. Prilikom testiranja potrebno je voditi računa o definisanju test slučajeva, pozitivnih i negativnih testova, kao i ispitivanju graničnih i izuzetnih slučajeva.

**Napomena:**

- Testiranje je moguće vršiti pomoću JUnit5 ili TestNG framework-a
- Selenium testove realizovati u Java programskom jeziku
- Za sve vrste testova za svakog studenta su definisane funkcionalnosti za koje bi trebalo da napiše testove

## Testiranje serverskog dela

U sklopu serverske (Spring) aplikacije potrebno je realizovati testove za sledeće funkcionalnosti:

- **Student 1:**
  - Funkcionalnost 2.4.1: Poručivanje vozila
- **Student 2:**
  - Funkcionalnost 2.7: Završetak vožnje
- **Student 3:**
  - Funkcionalnost 2.6.5: Zaustavljanje vožnje dok je u toku

Testirati sve klase koje omogućavaju izvršavanje pomenutih funkcionalnosti i to na sledeći način:

- **Klase REST sloja**
  - Testirati integracionim testovima
  - Obratiti pažnju na testiranje svih graničnih i izuzetnih slučajeva
- **Klase servisnog sloja**
  - Testirati jediničnim testovima
  - Obratiti pažnju na testiranje svih graničnih i izuzetnih slučajeva
- **Klase sloja repozitorijuma**
  - Testirati jediničnim testovima
  - Testirati sve metode, osim onih koje su direktno nasleđene iz JPA repozitorijuma

## Testiranje klijentskog dela

Potrebno je realizovati jedinične testove klijentskog sloja (Angular aplikacija) za sledeće funkcionalnosti:

- **Student 1:**
  - Funkcionalnost 2.2.3: Registracija vozača (bez registracije vozila) – testirati formu za registraciju vozača i proveriti slanje unetih podataka
- **Student 2:**
  - Funkcionalnost 2.8: Ocenjivanje vozila i vozača – testirati formu za ocenjivanje vozila i vozača i proveriti slanje unetih podataka
- **Student 3:**
  - Funkcionalnost 2.2.2: Registracija korisnika – testirati formu za registraciju korisnika i proveriti slanje unetih podataka

## E2E testiranje

**Preduslovi:** E2E testovi zahtevaju da backend (Spring Boot) i frontend (Angular na `http://localhost:4200`) budu pokrenuti, i da baza bude popunjena seed podacima (`mysql-seed.sql`). Lozinka za sve seed korisnike je `Password12345`. Bez pokrenutog backend-a, prijavljivanje u testovima neće uspeti.

Sledeće funkcionalnosti je potrebno testirati upotrebom Selenium alata:

- **Student 1:**
  - Funkcionalnost 2.4.3: Poručivanje vožnje iz omiljenih ruta
- **Student 2:**
  - Funkcionalnost 2.8: Ocenjivanje vozila i vozača
- **Student 3:**
  - Funkcionalnost 2.9.3: Filtriranje i sortiranje pregleda istorije

Prilikom testiranja obratiti pažnju na happy path, kao i na izuzetne slučajeve.
