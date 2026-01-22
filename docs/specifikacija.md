Projektni zadatak iz predmeta:

# Inženjerstvo serverskorg sloja  
Inženjerstvo klijentskog sloja  
Testiranje softvera  
Mobilne aplikacije  
Metodologije razvoja softvera

Softversko inženjerstvo i informazione tehnologije 2025/2026  
verzija 1.1

# 1. Namena systemd

Projektni zadatak predstavlja aplikaciju koja omogucava korisnicima da dobiju prevoz slično postojćem Uber-u. Uzimajuću u obzir nedostatak današnjeg taksi prevoza, ideja je da se maksimalno olakša transport korisnika uz redukciju interakcije sa prevoznikom kako bi se ceo proces ubrzao, bio konzistentniji i sigurniji.

# Postojcetiri vrste korisnika:

1. Neregistrovani korisnici - Mogu da vide osnovne informacije o aplikaciji sa mogucošcu da odaberu polazište i destinaciju i time se informišu o procenjenom vremenu i novcu neophodnom za obavljanje transporte.  
2. Registrovani korisnici - Mogu da zatraže vožnju uz dobijanje konstantnih notifikacija o promeni stanja zatražene vožnje, mogu da definišu više stanica izmeū početne i krajné tačke, mogu da prate sva vozila/o na mapi u svakom trenutku i dodele ocene vozaču/vozilu nakon vožnje. Narucivanje vožnje se moze zakazati i za budućnost kako bi se imao prioritet dodeljivanja vožnji u slučajevima gužve. Dodatno, korisnici mogu da vide kompletnu svoju istoriju vožnji sa mogućnosću pregleda izvestaja na nivou opsega datuma, da definišu omiljene rute kako bi imali mogućnost brzog biranja i u toku vožnje zatraže pomoć putem PANIC dugmeta koje javlja dispečeru da nešto sa vožnjomovie kako treba. Svaki korisnik ima mogućnost menjanja podataka na profilu i kontakitranja support-a za različita pitanja i nedoumium.  
3. Vozači - Automatski im se dodeljuje vožnja od straneSYSTEMA pri Čemu im sePokazuje polazište i destinacija. Kao i registrovani korisnici, mogu urečivati svoj profil (promene moraju biti odobrene od strane administratora), videti istoriju vožnji i generisati izveštaje na nivou opsega datuma i imaju pristup PANIC dugmetu koje označava da postoji problem sa zadatom vožnjom. Prilikom dodeljivanja i tokom vožnje imaju opcju da istu odbiju/prekinu uz obavezno naven den razlog odbijanja/prekidanja. Samim prijavljivanjem na aplikaciju, vozač automatski postaje dostupan za vožnje, dok se odjavom postije suprotan efekat. Isti ima opcju da manuelno promeni svoje stanje u aktivan/neaktivan. Takoše vozač postaje nedostupan ako mu broj radnih sati u toku dana preće 8h.  
4. Administratori - Kreiraju naloge vozačima i u svakom trenutku mogu videti informacije i stanje vožnje bilo kog vozača. Takoš imaju pristup istoriji svakog vozača i mogu generisati globalne izvestaje, kao i izvestaje za svakog posebno. Mogu blokirati korisnike i vozače, reagovati na PANIC notifikacije i pruzaju podršku u vidu live chat-a 24/7.

# 2. Funkcionalni zahtevi

# 2.1. Prikaz informacija neregistrovanim korisnicima

# 2.1.1 Prikaz informacija (Student2)

Prva stranica koju (neprijavljeni) korisnik vidi je početna stranica aplikacije na kooj se mogu videti sva trenutno aktivna vozila sa njihovim položajem na mapi, pri Čemu je za svako vozilo naznačeno da li je zauzeto ili trenutno slobodno.

# 2.1.2 Procena vožnje (Student3)

Na početnoj stranici sa mapom, postoji dugme koje otvara formu u koju se mogu uneti adresa polaziša i destinacje. Nakon unosa podataka, system korisniku prikazuje odabranu rutu na mapi i procenjeno vreme za vožnju.

# 2.2. Registracija korisnika i prijavljivanje na systemd

# 2.2.1 Logovanje (Student3)

Pomoću korisnikovejejadresi lozinke moze se izvršiti prijava na system.

Na stranici za prijavu, korisnik ima opciju da ukolko je zaboravio lozinku, uradi njen reset putemjejka koji ce ga odvesti na stranicu na kojoj moze uneti novu lozinku.

Vozači samom prijavom na system postaju automatski dostupni za dodeljivanje vožnji, dok se odjavom postiže suprotan efekat. Vozači se ne mogu odjaviti ukoliko trenutno imaju aktivno vožnju.

Svoje stanje mogu manuelno promeniti u aktivan/neaktivan u svakom trenutku iako su prijavljeni na system. Ukoliko vozač promeni stanje na neaktivan u toku vožnje, postace neaktivan nakon iste i system ga neće moci ponuditi narednim korisnicima. Nakon promene stanja ce imati mogucnost da se i odjavi izSYSTEMA.

# 2.2.2 Registracija korisnika (Student3)

Ukoliko korisnik još uveknecte registrovan na systemd, aželi da koristi napredne funkcjie aplikacije, mora prvo da se registruje na odgovarajućoj stranici.

Registracija obuhvata uno很不错 adrese, lozinke, imena, prezimena, adrese stanovanja i broja Telefona. Lozinka se unosi u dva polja da bi se otežalo pravljenje grešaka prilikom odabira lozinke. Prilikom registracije slika-nine obavezna da se unese. Ako korisnik ništa ne unese, staviti neku predefinisanu sliku.

Registracija obuhvata i slanjejej mejla na dato adresu sa linkom za aktivaciju korisnika koji traje 24h. Korisnik ne moze da se prijavna aplikaciju dok se njegov nalog ne aktivira posecivanjem linka koji je dobio u mejlu.

# 2.2.3 Registracija vozača (Student1)

Administratori su definisani unapred i oni mogu kreirati naloge vozačima, gde se pored podataka o vozaču definišu i podaci o samom vozilu.

Podaci o vozaču odgovaraju podacima za obične korisnike (tačka 2.2.2).

Podaci koje treba popuniti o vozilu su: model vozila, tip vozila (standardno, luksuzno, kombi), registarske tablice, broj mesta, oznaka da li vozilo omogucava prevoz beba (da/ne), oznaka da li vozilo omogucava prevoz kućnih ljubimaca (da/ne).

Nakon što administrator kreira nalog, vozaču se automatski šalje mejl sa Jednokratnim linkom za aktivaciju profila, koji važi 24 ča. Mejl ne sadrži lozinku, već samo obaveštenje o kreiranju naloga i link za postavljanje lozinke.

Klikom na link za aktivaciju otvara se stranica za inicijalno postavljanje lozinke, pri Čemu se nova lozinka unosi u dva polja (lozinka i potvrda lozinke). Nakon uspešnog postavljanja lozinke, aktivacioni link postaje nevažeće.

Vozač se nakon toga uSYSTEMPRIJAVljuje pomoću svoje mejl adrese i lozinke.

# 2.3. Profil korisnika (Student1)

Registrovani korisnik, admin i vozač su u mogućnosti da pregledaju i ažuriraju svoje lične podatke na stranici za prikaz svog profila.

Pored osnovnih podataka, korisnici mogu izabrati sliku koja ce se prikazivati vozačima kada isti dobiju neku vožnju od strane systemd iili putnicima ako je u pitanju vozač. Ako korisnik ne unese sliku, postaviti neku predefinisanu sliku.

Vozači pored osnovnih podataka, mogu da vide koliko su sati trenutno aktivni u poslednja 24h i da pregledaju informacije o vozilu.

Promene nastale od strane vozača moraju biti odobrene od strane administratora. Šalje se zahtev administratorima za promenu informacija na profilu, koji oni mogu da pregledaju i da odobre ili odbiju. Nakon odobravanja promene postaju vidljive.

Na stranici profila se moze izabrati opcija za promenu lozinke.

# 2.4. Poručivanje vožnje

# 2.4.1 Poručivanje vožnje (Student1)

Ulogovanom korisniku se na početnoj stranici prikazuje mapa u istom obliku kao i neulogovanom sa razlikom u više opcija koje se mogu definisati prilikom poručivanja vožnje.

Pored definisanja polaziša i destinacije, ulogovani korisnik ima mogućnost da definiše više stanica izmedu početne i krajné tačke pri Čemu redosled igra bitnu ulogu jer definiše redosled u kom vozač mora obići navedene tačke.

Korisnik moze ulinkovati više drugih putnika prema njihovim mejl adresama i da na taj način ostali putnici mogu pratiti detailje o vožnji (dodatno u 2.4.2). Pretpostaviti da svi korisnici putuju od polazišta do destinacije i da vožnju placă kreator vožnje.

Nakon što je putanja izabrana, korisnikmöze definisati neke dodatne stavke bitne za izbor vozača: izbor tipa vozila, da li se prevoze bebe ili kućni ljubimci.

Cena se računa po formuli cena\_po\_tipu\_vozila + broj\_kilometara * 120.

Sistem proverava da li postoje dostupni vozači i ako postoje automatski dodeljuje vožnju vozaču i putnicima. Oblatiti pažnju na sledeće:

- Ako ne postoji nijedan vozač prijavljen/aktivan, vožnja se odbija i korisniku stiže notifikacija da trenutno nema aktivnih vozača.  
- Ako su svi vozači trenutno zauzeti i ako imaju već zakazanu buduću vožnju, takоće se vožnia odbija uz slanje notifikaciţe da trenutno nema aktivnih vozača.  
- Ako ima slobodnih vozača,SYSTEM bira najblizeg, a ako su svi zauzeti, bira se onaj koji je najblži polazištu i najblži zavsetku trenutne vožnje (preostalo mu je još 10 minuta prethodne vožnje). Korisniku se šalje notifikacija o dodeljenoj vožnji.  
- Ako vozač ima više od 8 radnih sati u poslednja 24 šasa, ne postoji mogućnost da mu system dodeli vožnju.

Kada je vozač uspešno pronažen, šalje se notifikacija vozaču o novoj vožnji. Korisnik koji je narucio vožnju se isto šalje notifikacija da je porudžbina prihvaćena. Ako je poručivanje neuspešno, korisnik koji je narucio vožnju dobija notifikacija o neuspešnoj vožnji.

Vožnja se moze zakazati i za budućnost (npr. putnik u 10:00 zakaže vožnju za 15:00) pričemu unapred zakazane vožnje imaju prioritet prilikom dodeljivanja vozila. Vožnja se moze zakazati najviše 5 Časova unapred.

Na 15 minuta do početka vožnje i na svakih 5 nakon toga, korisnik dobija notifikaciju da je zakazao vožnju kao podsetnik. Nakon početka vožnjeicates potrebno da se dobija notifikacija.

# 2.4.2 Notifikacije ulinkovanih putnika (Student2)

Ulinkovani putnici (ako ih ima) dobijaju要比i notifikaciju (samo registrovani korisnici u aplikaciji) da su dodati na voznju i da je voznja prihvaćena, u slučaju da je system pronašao podobnog vozača.

Klikom na link u mejlu ili klikom na notifikaciju putnici odlaze na posebnu stranicu gde im se pruža mogucnost praćenja vožnje (dodatno u 2.6.2).

Kada vozilo stigne na odredište, šalje se ponovo mejl i notifikacija (samo registrovani korisnici) svim putnicima da je vožnja uspešno završena.

# 2.4.3 Poručivanje vožnje iz omiljenih ruta (Student1)

Prilikom poručivanja vožnje, korisnik ima mogucnost da pregleda i da brzo odabere neku rutu iz svojih omiljenih ruta, gde ce mu se vec popuniti polazište, odredište i ostale stanice u ruti. Na stranici sa istorijom vožnji, treba omoguciti da se neka ruta stavi u omiljene i izbaci.

# 2.5. Otkazivanje vožnje (Student3)

Vozač nakon dodeljene vožnje od straneSYSTEMA, preNgu što putnici uduu vozilomože otkazati vožnju pri Čemu mora navesti razlog otkazivanja (npr. putnika nema na zadatom polazištu ili zdravstveni problem vozača usled Čega mora završiti smenu itd.).

Korisnik koji je poručio vožnju moze da otkaže vožnju 10 minuta pre početka iste.

# 2.6. Obavljanje vožnje

# 2.6.1 Početak vožnje (Student1)

Nakon što su svi putnici pristupili vozilu, vozač je u obavezi da označi početak vožnje. Aktivni putnici ne mogu poručivati nove vožnje dokle god se trenutna ne završi.

# 2.6.2 U toku trajanja vožnje (Student2)

U toku iste voznje, svaki od putnika je u mogucnosti da pristupi stranici gde im se pruza mogucnost pracenja voznje (lokacije vozila na mapi) uz prikaz vremena neophodnog da vozilo stigne pri cemu se vreme ažurira kako se vozilo približava destinaciji.

U slučaju da vozač ide nekim neadekvatnim putem (po proceni korisnika), svim putnicima se nudi opcija da prijave nekonzistentnost vozača (kao napomenu). Potrebno je prikazati malu formu gde se unosi tekst. Ove prijaveće se prikazivati u izvestajima i prilikom pregleda istorija vožnji.

# 2.6.3 PANIC dugme (Student3)

Ako se dešava nešto nepredvideno, svaki putnik moze kliknuti na PANIC dugme, čime se centrali (administratorima) šalje obična i zvučna notifikacija da postoji ozbiljan problem sa vožnjom i vozilo se na mapi označava na poseban način tak do bude što učljivije.

Vozači takode imaju pristup PANIC dugmetu.

Dalje postupanje nakon notifikacije, rešavaju administratori takо što zovu vozača, policiju, hitnu pomoć itd. (u sistemu ne treba podržati zvanje policije i vozača, to su akcije koje izvršavaju ljudi). PANIC dugme je samo dugme u sistemu, koje obavešta administatore da postoji opasnost.

Administrator moze da pregleda PANIC notifikacije. Prilikom aktiviranja PANIC notifikacije, administrator dobija običnu i zvučnu notifikaciju.

# 2.6.5 Zaustavljanje vožnje dok je u toku (Student3)

Korisnik koji se vozi moze usmeno da zatraži da se auto zaustavi. Vozač u tom slučaju, zaustavlja vozilo na najblžem sigurnom mestu i klikom na dugme zaustavlja tok vožnje u aplikaciji. Aplikacija kupa podatke o mestu zaustavljanja i vremenu, preračunava cenu i Čuva podatke o vožnji, takao da menja adresu odredišta u novu adresu gde je auto zaustavljen.

# 2.7. Zavrětak vožnje (Student2)

Nakon što je vožnja obavljena i putnici su izašli iz vozila, vozač označava da je vožnja gotova i placěna u samom vozilu. Time vozač prelazi u dostupno stanje, ako nema drugu zakazanu vožnju. U slučaju postojanja zakazane, učitavaju mu se novi podaci i kreće ka novom polazištu. Ako nema dodeljenu vožnju, vozač ima opciju da ode na stranicu na kojoj vidi buduce (zakazane) vožnje. Putnicima stije mejl i notifikacija o završenoj vožnji, uz mogućnost ocene vožnje i ponovo mogu porucivati nove vožnje.

# 2.8. Ocenjivanje vozila i vozača (Student2)

Nakon završetka, osobi koja je poručila vožnju, nudi se opcija da ocene vozilo, vozača i ostave komentar. To mogu uraditi odmah nakon vožnje ili odlaskom na pregled istorije vožnji odakle mogu naknadno da ostave ocenu.

Rok za ostavljanje ocene je 3 dana od završetka vožnje. Ako rok istekne, ISTA SE smatra neocenjenom.

# 2.9. Pregled istorije vožnji

# 2.9.1 Registrovan korisnik (Student3)

Odlaskom na stranicu za pregled istorije vožnji, izlistavaju seiste sortirane prema datumu od najskorije do najstarije. U tabelnom prikazu se izlistavaju ruta koja je obavljena, datum početka i kraja vožnje i sortiranje se moze vršiti prema bilo kom polju. Istorija se moze filtrirati prema datumu nastanka.

Za svaku se moze videti i detaljni prikaz koji otvara mapu sa označenom rutom i dodatno se prikazuju podaci o vozaču, podaci o prijavama nekonzistentnosti vožnje, ocenama vožnje i opcjijom da se ponovo izabere istra ruta za poručivanje odmah, ili kasnije.

U mobilnej aplikaciji implementariti senzor za detektovanje shake dogaada pri cemu se shake-ovanjem ureda naizmenično sortira istorija vožnji po datumu.

# 2.9.2 Vozač (Student2)

Vozači takоğe imaju mogučnost pregleda sopstvene istorije, sa razlikom u tome što im se prikazuju informacije o svim putnicima. U okviru jeder vožnje treba prikazati kada je počela, kada se završila, polazište i odredište, da li je otkazana i od strane koga, koliko je koštala i da li se desiloPokretanje PANIC dugmeta. Istorija se moze filtrirati prema datumu nastanka.

# 2.9.3 Administrator (Student3)

Administratori mogu videti istoriju bilo kog vozača ili putnika. Izlistavaju se vožnje sortirane prema datumu od najskorije do Najstarije. Izlistavaju se sledeći podaci: ruta koja je obavljena, datum početka i kraja vožnje, polazište i odredište, da li je otkazana i od strane koga, koliko je koštala i da li se desiloPokretanje PANIC dugmeta. Istorija se moze filtrirati prema datumu nastanka. Sortiranje se moze vršiti prema bilo kom polju.

Za svaku se moze videti i detaljni prikaz koji otvara mapu sa označenom rutom i dodatno se prikazuju podaci o vozaču, putnicima, prijavama o nekonzistentnosti vožnje, ocenama vožnje i opcjom da se ponovo izabere ista ruta za porucivanje odmah, ili kasnije.

# 2.10 Generisanje izveštaja o prethodnim vožnjama (Student1)

Svi korisnici imaju mogućnost da na osnovu definisanog opsega datuma dobiju grafove koji prikazuju broj vožnji po danima, broj preşenih kilometara, količinu potrošenog/zaraadenog novca za sopstvene vožnje. Uz te podatke neophodno je prikazati i kumulativnu sumu za opseg kao i prosek.

Administratori dodatno imaju mogućnost da te podatke vide na jeder nom grafu za sve vozaće ili putnike, ili da odaberu samo Jednu osobu i za nju prikažu podatke.

# 2.11 Live podrška (Student2)

U svakom trenutku (bilo da je voznja u toku ili ne) i vozači i putnici mogu kontaktirati support za dodatna pitanja. Komunikacija se odvija u obliku chat-a. Administratori su ti koji senalaze sa druge strane istog. Chat treba da pamti istoriju tog razgovora, tj. i korisnik i administrator mogu da vide prethodne poruke. Nije potrebno kreirati novi Čet u zavisnosti od vožnje, nego svaki korisnik ima po jeder chat sa administratorom.

# 2.12. Blokiranje korisnika i ostavljanje napomena (Student1)

Administratori mogu u bilo kom trenutku da blokiraju vozača ili nekog putnika. Prvi postaju nedostpuni za dodeljivanje vožnji, dok drugi ne mogu porucivati nove vožnje.

Takoše, administratori mogu ostaviti napomenu za vozače ili putnike koja pojasnjava zašto su blokirani. Napomena se pojavljuje kao poruka prilikom pokušaja porucivanja voženje kod korisnika, dok kod vozača stoji na profilu.

# 2.13. Pregled stanja vožnje (Student2)

Administratoredo pregleda stanje voznje koja trenutno traje, bilo kog vozača. Na stranici ima pretragu po imenu vozača i odabirom moze da pregleda sve informacije, kao sto su vreme polaska, vreme dolaska ili trenutan položaj itd.

# 2.14. Definisanje cene vožnje (Student2)

Administrator moze da definiše i-menja cenu vožnje. Prilikom definisanja treba obratiti pažnju na tip vozila (standardno, luksuzno, kombi). Nije potrebno pamtiti istoriju cena, neka se u računima Čuvaju podaci o ukupnej ceni i ceni koja je važila u trenutku kreiranja računa.

# 3. Nefunkcionalni zahtevi

- Za mapu i lokacije se mogu koristiti proizvoljni servisi. Preporucujemo OpenStreetMap alate. Za putanje videti https://www.openstreetmap.org/directions  
- Simulaciju kretanja vozila po mapi kada vozač nema vožnju je moguce definisati na proizvolan način.  
- Log out je obavezan za sve korisnike.  
Vreme koje je izražneno u satima i minutima, skratiti na minute i sekunde zbog demonstracione.  
- Za prikaz svake fungcionalnosti potrebno je obezbediti test podatke pre odbrane, kako bi se što efikasnije mogle predstaviti aplikacije. Ako neko ne bi imao test podatke, računaće se da taj deoicates odražen.

Inženjerstvo klijentskog sloja:

- Tecnologije koje je obavezno koristiti: Angular (TS, HTML, CSS), Angular Material ili Bootstrap (ili sličnu biblioteku), Figma.  
- Dizajn (CSS stilovi) se mogu slobodno uraditi poŽelji tima. Nije dozvoljeno da aplikacija bude implementirana bez CSS-a (ovo se odnosi na svaku componentu).  
- Obavezno je koristiti Angular componente, rutiranje, Angular AuthGuard itd. Koristiti linter i klase/interfeise kao modele.  
- Bezbednost (logovanie i registracija) se implementira putem JWT-a.  
- Mape integratis kroz Leafelt (openstreet) po uzoru sa vežbi.

Inženjerstvo softverskog sloja:

- Serverski sloj je neophodno odraditi koristeči Javu i Spring Boot radni okvir.  
- Za bazu podataka koristiti bilo koju relacionu bazu podataka, kao što su H2, PostgreSQL.  
- Za slanje email-aijke obezbeden poseban servis. Możete koristiti sopstveni nalog. Preporučujemo SendGrid: https://sendgrid.com  
- Notifikacije treba Čuvati u bazi, kako bi korisnici möglich da ih vide naknadno i da reaguju na njih.

Mobilne aplikacije:

- Mobilnu Android aplikaciju je neophodno ograditi koristecki Java programski jezik.  
- Dizajn applikacja seMZe uraditi po czelji tima, ali da bude slican kao klijentska aplikacja.  
- Za lepši izgled componenti može se koristiti Material Design 3 biblioteka https://m3 material.io/components.  
Softverska arhitektura mobilne appliekij bi trebalo da se sastoji od:

O UI Layer - sloj korisničkog interfejsa koji prikazuje podatke aplikacije na ekranu.  
$\circ$  Domain Layer (optional) - dodatan sloj domena koji služi da biste pojednostavili i ponovo iskoristili interakcije izmeū korisničkog interfeisa i slojeva podataka.  
Data Layer - sloj podataka koji sadrži poslovnu logiku vaše aplikacije i izlaže podatke aplikacije.

- Za mapu i lokacije se mogu koristiti proizvoljni servisi. Za prikaz mape preporucujemo OpenStreetMap, Mapbox ili Google Maps SDK.  
U aplikaciji treba da imate navigaciju koja je uvek vidljiva na ekranu.  
- Koristiti ugräden systemd za notifikacje u Android-u. Notifikacje Čuvati u bazi;kako bi se obezbezbedilo naknadno reagovanje na pristigle notifikacje.  
- Podešavanja aplikacje Čuvati u SharedPreferences.  
- Prilikom povezivanja aplikacije sa Inženjerstvo serverskog sloja, neophodno je voditi se API dokumentacionjom koju kreirate na drugom predmetu.  
- Ukoliko se ne radi deo vezan za Inženjerstvo serverskog sloja, moguce je implementarati zasebno mobilnu aplikaciju. Od baza podataka moze da se koristi SQLite baza ilii Firebase platforma.  
- Możete iskoristiti bilo koju biblioteku koja ce vam ubrzati posao, ali morate znati da objasnite zašto i kako je koristite!

Metodologije razvoja softvera:

Trello, Sprint retrospective  
- Aplikaciju razvijati na engleskom jeziku, sve tabele koje su vezane za MRS predmet obavezno je popuniti na engleskom jeziku.  
- Potrebno je napraviti uvodni dokument, koji sadrži plan rada po nedeljama (on kasnije要考虑 biti podložan promenama), sadrži plan promene uloga po nedeljama (product owner, scrum master) i ciljeve.  
- Potrebno je praviti dokumente retrospektive i sprint review-a  
- Obratiti pažnju na kriterijume prihyvatliivosti svih taskova  
- Burndown chart predati na kontrolnim tačkama, gdećete prižati progres tima po taskovima  
- Prezentacija kontrolnih tački treba da sadrži video snimak (5-7 minuta) gde se prikazuji funckionalnosti softvera  
- Dodati asistenta na trello, rajtarovnatasa@gmail.com i dokunte slati na uns maior natasarajtarov@uns.ac.rs

Testiranje softvera:

- Za testiranje Java koda koristiti JUnit ili TestNg biblioteku  
- Za testiranje Angular koda koristiti Jasmine radni okvir  
- Za E2E testiranje aplikacije koristiti Selenium.

# 5. Zahtevi za kontrolne tačke

<table><tr><td>Predmet</td><td>Kontrolne tačke</td><td>Datum</td><td>Student 1</td><td>Student 2</td><td>Student 3</td></tr><tr><td rowspan="3">ISS</td><td rowspan="2">KT1</td><td rowspan="2">29.12.2025.</td><td></td><td></td><td></td></tr><tr><td colspan="3">Potrebno je implementirati sve klase za kontralor koje imaju sve endpointe neophodne za rad sa aplikacijom u skladu sa specifikacijom.</td></tr><tr><td>KT2</td><td>28.1.2026.</td><td>2.2.3, 2.3, 2.4.1, 2.4.3, 2.6.1</td><td>2.1.1, 2.6.2, 2.7, 2.8, 2.9.2</td><td>2.1.2, 2.2.1, 2.2.2, 2.5, 2.6.5, 2.6.3</td></tr><tr><td rowspan="4">IKS</td><td rowspan="2">KT1</td><td rowspan="2">22.12.2025. I 23.12.2025.</td><td>2.3 (samo osnovne info o profilu)</td><td>2.9.2 i početni navigazioni bar koji ima dugmiće sa kojim se prebacujemo na componente koje su razvjene za prvu KT.</td><td>2.2.1 i 2.2.2</td></tr><tr><td colspan="3">- Svaki student treba da dodatno okvirno dizajnira 50% svojih funkcionalnosti u Figmi (iz celog projekta). Tim treba da razvjedizajn u Jednom fajlu gde ce okvirno definisati boje, oblike, font itd. - Za sve tačke (2.3, 2.9.2, 2.2.1 i 2.2.2) je potrebno realizovatiismo UI tj.necte potrebno ništa uvezivati sa serverom.</td></tr><tr><td rowspan="2">KT2</td><td rowspan="2">28.01.2026</td><td>2.2.3, 2.4.1, 2.4.3, 2.6.1</td><td>2.1.1, 2.6.2, 2.7, 2.8</td><td>2.1.2, 2.5, 2.6.5, 2.6.3</td></tr><tr><td colspan="3">- Svaki student treba i da završi funkcionalnosti iz prve KT u smislu da ih uveže sa serverskim slojem i da budu funkcionalne.</td></tr><tr><td>TS</td><td>ODBRANA</td><td></td><td></td><td></td><td></td></tr><tr><td rowspan="4">MA</td><td rowspan="2">KT1</td><td rowspan="2">30.12.2025. u terminu predavanja</td><td>2.3</td><td>2.9.2</td><td>2.2.1 i 2.2.2</td></tr><tr><td colspan="3">Pored ovih tačaka, zajednički treba da odradite navigaciju u aplikaciji koja bi trebala biti vidljiva na svim stranicama.Potrebno je odraditi sono GUI aplikacije. Dizajn (boje, font, oblike.) uskladiti sa IKS predmetom.</td></tr><tr><td rowspan="2">KT2</td><td rowspan="2">10.02.2026. u terminu predavanja.</td><td>2.3</td><td>2.9.2</td><td>2.2.1 i 2.2.2</td></tr><tr><td colspan="3">Potrebno je odraditi funkcionalnosti u potpnosti.</td></tr><tr><td rowspan="2">MRS</td><td>KT1</td><td>16.1.2026</td><td colspan="3">Zahtevi za KT1 sa ISS i IKS predmeta + dogovor sa vežbi.</td></tr><tr><td>KT2</td><td>11.2.2026</td><td colspan="3">Zahtevi za KT2 sa ISS i IKS predmeta + dogovor sa vežbi.</td></tr></table>
