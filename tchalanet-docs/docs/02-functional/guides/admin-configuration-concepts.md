# Konfigirasyon admin - espas kliyan, jwèt, tiraj ak limit

Dokiman sa a eksplike sa yon admin dwe konprann pou konfigire espas kliyan li
anvan li bay machann yo aksè pou vann.

## Objektif konfigirasyon an

Konfigirasyon admin lan dwe reponn 5 kesyon:

1. Ki espas kliyan / tenansye bòlèt k ap opere?
2. Ki machann / machin ki gen dwa vann?
3. Ki jwèt ki disponib?
4. Ki tiraj / draw channels ki ouvè pou vann?
5. Ki limit ki pwoteje risk la?

## Checklist espas kliyan anvan test

- Espas kliyan an egziste ak bon non, timezone ak lajan.
- Admin prensipal la ka konekte.
- Machann yo ak machin POS yo kreye.
- Jwèt ki nesesè yo aktive.
- Opsyon jwèt ki nesesè yo vizib sou POS.
- Draw channels yo aktive.
- Tiraj yo jenere epi gen estati ki kòrèk.
- Limit minimòm yo mete.
- Maryaj gratis konfime: aktif oswa inaktif.
- Yon machin POS test ka vann yon tikè bout-a-bout.

## Konfigire yon jwèt

Yon jwèt nan espas kliyan an gen konfigirasyon sa yo:

| Paramèt | Sa li kontwole |
|---|---|
| Non afichaj | Non admin ak machann yo wè a. |
| Lòd afichaj | Ki pozisyon jwèt la pran nan lis la. |
| Vizib sou POS | Si machann nan wè jwèt la. |
| Miz minimòm | Pi piti miz ki aksepte. |
| Miz maksimòm | Pi gwo miz ki aksepte. |
| Disponibilite | Si jwèt la disponib selon jou/lè. |
| Jou disponib | Egzanp `MON-SUN`, `MON-SAT`. |
| Lè kòmansman / fen | Fenèt lokal pou jwèt la. |

Admin lan pa dwe konprann sa tankou "kreye jwèt lib". Nan sistèm lan, Tchalanet
sipòte yon lis jwèt. Admin lan aktive, klase, limite oswa kache jwèt sa yo.

Miz minimòm ak miz maksimòm se borne jwèt la. Tenansye a konfigire yo pou chak
jwèt selon jan li vle jere biznis li.

## Konfigire opsyon jwèt

Pou jwèt ki gen opsyon, admin lan ka ajiste:

- ki opsyon ki aktive;
- ki opsyon ki vizib sou POS;
- ki opsyon ki chwazi kòm règ debaz;
- lòd afichaj opsyon yo;
- fason seleksyon an fèt.

### Fason seleksyon an fèt

Nou sipòte 2 mòd prensipal:

| Mòd | Sans | Lè pou itilize |
|---|---|---|
| Explicite | Machann nan chwazi opsyon an klèman sou POS la. | Lè admin lan vle machann nan pran desizyon an, pa egzanp chwazi ant `Egzak` ak `Dezòd / box`. |
| Implicite | Sistèm lan chwazi opsyon ki pi byen matche ak nimewo a oswa jwèt la. | Lè admin lan vle vann lan rete rapid epi sistèm lan aplike règ ki koresponn lan selon konfigirasyon an. |

Genyen tou yon mòd ak asistans kote machann nan ka chwazi, men sistèm lan ka
pwopoze opsyon ki pi apwopriye a si sa aplikab.

Konfigirasyon debaz:

- Bòlèt itilize lo rezilta yo pou kalkile peman an.
- Maryaj, Loto 3, Loto 4 ak Loto 5 ka itilize mòd implicite.

## Opsyon aktyèl pa jwèt

| Jwèt | Sa kliyan an bay | Opsyon / règ | Egzanp pou admin verifye |
|---|---|---|---|
| Bòlèt | Yon nimewo 2 chif | Premye lo, dezyèm lo, twazyèm lo kòm pozisyon rezilta ki ka peye | Kliyan jwe `45`. Si `45` soti nan rezilta a, peman an depann de lo kote li soti la. |
| Maryaj | De nimewo 2 chif | Lòd egzak, ranvèse / doub | Kliyan jwe `12-45`. Lòd egzak mande menm lòd la. Ranvèse ka pèmèt `45-12` selon règ ki aktive. |
| Maryaj gratis | De nimewo 2 chif sou liy gratis | Menm prensip Maryaj, men kòm pwomosyon | Si règ la aktif, sistèm lan ajoute liy gratis la pandan preparasyon tikè a. |
| Loto 3 | Yon nimewo 3 chif | Egzak, dezòd / box, egzak + pèmitasyon | Kliyan jwe `123`. Egzak mande `123`; dezòd / box ka aksepte menm chif yo nan lòt lòd. |
| Loto 4 | Yon nimewo 4 chif | Egzak, dezòd / box, 2 premye chif, 2 dènye chif, egzak + pèmitasyon | Kliyan jwe `1234`. 2 premye chif gade `12`; 2 dènye chif gade `34`. |
| Loto 5 | Yon nimewo 5 chif | 1e lo + 2e lo, 1e lo + 3e lo, melanje 1e/2e/3e lo | Kliyan jwe `12345`. Règ la di ak ki lo rezilta sistèm lan dwe konpare nimewo a. |

Egzak + pèmitasyon pou Loto 3 ak Loto 4 disponib, men li pa aktive nan
konfigirasyon debaz la.

## Odds ak barèm

Odds / barèm yo se règ peman yo. Yo pa menm bagay ak limit.

Egzanp barèm:

| Jwèt | Egzanp barèm |
|---|---|
| Bòlèt premye lo | 50x |
| Bòlèt dezyèm lo | 20x |
| Bòlèt twazyèm lo | 10x |
| Maryaj | 1000x |
| Loto 3 egzak | 500x |
| Loto 4 egzak | 5000x |
| Loto 5 | 25000x |
| Maryaj gratis | Montan fiks 500 G selon règ aktyèl yo. |

PDF admin lan dwe eksplike:

> Barèm di konbyen nou peye. Limit di konbyen nou kite moun vann.

## Konfigire draw channel

Yon draw channel se kanal espas kliyan an itilize pou vann yon tiraj.

Li gen:

- kòd kanal la;
- non kanal la;
- result slot ki asosye a;
- draw time;
- cutoff time;
- timezone;
- estati active;
- jwèt ki pèmèt sou kanal la.

Pou yon admin, konfigirasyon draw channel vle di:

1. chwazi kanal ki dwe vann;
2. verifye lè tiraj la;
3. verifye cutoff la;
4. verifye jwèt ki disponib sou kanal la;
5. aktive oswa dezaktive kanal la;
6. teste ke kanal la parèt nan POS.

## Konfigire draw

Yon draw se yon tiraj konkrè pou yon dat.

Admin dwe konnen estati yo:

| Estati | Sans |
|---|---|
| `SCHEDULED` | Tiraj la pwograme men poko ouvè. |
| `OPEN` | Machann ka vann sou tiraj la. |
| `CLOSED` | Vant fèmen, ap tann rezilta. |
| `RESULTED` | Rezilta antre. |
| `SETTLED` | Kalkil / règleman fèt. |
| `CANCELED` | Tiraj la anile. |
| `ARCHIVED` | Tiraj la soti nan operasyon aktif. |

Libellé kliyan:

- `Tiraj ki louvri`;
- `Tiraj ki fèmen`;
- `Konfigire tiraj yo`.

## Konfigire limit

Pou kòmanse, admin dwe mete limit senp epi konprann yo.

Egzanp limit operasyonèl:

- miz max pou yon liy;
- miz max pou yon tikè;
- kantite liy max nan yon tikè;
- miz max sou menm nimewo pou menm tiraj;
- bloke yon nimewo pou yon tiraj;
- bloke yon kalite pari.

Scope posib:

| Scope | Lè pou itilize |
|---|---|
| Espas kliyan | Règ jeneral pou tout kliyan an. |
| Seller terminal | Règ pou yon machin / machann espesifik. |
| Draw channel | Règ pou yon kanal tiraj espesifik. |
| Agent | Règ pou itilizatè / ajan espesifik si workflow la itilize li. |

## Règ espesyal

Règ espesyal se yon eksepsyon sou règ jeneral espas kliyan an.

Prensip la:

- tenansye a mete yon règ debaz;
- yon machann, yon machin POS, yon jwèt oswa yon tiraj ka gen yon règ diferan;
- sistèm lan aplike règ ki pi espesifik la lè li prepare oswa konfime vant lan.

Egzanp:

| Règ jeneral | Règ espesyal |
|---|---|
| Komisyon debaz tenansye a se `10%`. | Machann Jean gen komisyon `12%`. |
| Barèm Bòlèt debaz la se `50x / 20x / 10x`. | Yon machann gen dwa peye `60x / 25x / 15x`. |
| Limit sou yon nimewo se 2,000 G pou tout machann. | Yon machin POS gen limit 1,000 G sou menm nimewo a. |

Admin ka bezwen règ espesyal tou lè:

- founisè a pa bay rezilta;
- yon tiraj bezwen fèmen oswa anile;
- yon nimewo dwe bloke pou risk;
- yon result slot gen jou founisè a fèmen.

Prensip:

- toujou ekri rezon an;
- verifye efè a sou POS;
- verifye rapò apre sa;
- pa itilize règ espesyal kòm operasyon nòmal chak jou.

## Sa pou admin teste apre konfigirasyon

- POS wè sèlman jwèt ki dwe vann.
- POS wè sèlman draw channels aktif.
- Yon tikè Bòlèt ka prepare epi konfime.
- Yon tikè Maryaj ka prepare epi konfime.
- Si Maryaj gratis aktif, liy gratis la parèt nan prepare.
- Limit max line bloke oswa avèti jan li dwe fè.
- Fèmen yon draw retire l nan vant.
- Rezilta founisè oswa rezilta manyèl parèt nan ekran rezilta.
- Rapò admin montre tikè, vant brit, ganyan, komisyon ak net.
