# Glosè kliyan - konsèp Tchalanet

Dokiman sa a eksplike mo kle yo pou yon admin, yon machann, oswa yon kliyan ki
ap teste Tchalanet. Li sèvi kòm baz pou PDF vizyèl yo ak paj èd yo.

## Espas kliyan / tenansye bòlèt

Yon espas kliyan se espas travay yon kliyan nan Tchalanet. Li ka yon antrepriz oswa
yon moun. Li gen pwòp machin POS li, machann li, jwèt li, tiraj li, limit
li ak rapò statistik li.

Nan langaj bòlèt ann Ayiti, moun oswa antrepriz ki opere bòlèt la ka rele
**tenansye bòlèt**. Nan sistèm lan, se espas kliyan sa a ki regroupe tout
operasyon tenansye a.

Nan PDF ak paj èd yo, nou itilize **espas kliyan** pou pale de kont lan nan
Tchalanet, epi **tenansye bòlèt** pou pale de moun oswa antrepriz ki opere
bòlèt la.

Nan sistèm nan, lè nou kreye yon espas kliyan, nou mete omwen:

- kòd tenansye a;
- non tenansye a;
- kalite tenansye a;
- timezone;
- lajan;
- pousantaj komisyon debaz;
- profil konfigirasyon an;
- premye admin lan.

## Machann ak machin POS

Pou kliyan an, gen 2 konsèp senp:

| Mo | Sans pratik |
|---|---|
| Machann | Moun ki sèvi ak machin nan pou vann tikè yo. |
| Machin POS / tèminal POS | Menm bagay: aparèy oswa pwen vant kote tikè yo vann. |

Nan sistèm lan, machin POS / tèminal POS la gen yon kòd, yon non, enfòmasyon
machann nan, PIN, komisyon, epi yon estati tankou aktif oswa bloke.

Pou PDF yo, nou ka di:

> Machin POS ak tèminal POS se menm bagay. Se pwen kote machann nan konekte pou
> vann tikè yo.

## Jwèt

Yon jwèt se kalite pari machann nan ka vann.

Nou sipòte 6 jwèt:

| Jwèt | Sa li vle di |
|---|---|
| Bòlèt | Pari sou yon nimewo 2 chif. Kliyan an jwe nimewo a; apre rezilta a, si nimewo a soti nan premye lo, dezyèm lo oswa twazyèm lo, sistèm lan aplike barèm ki koresponn lan. Egzanp: kliyan an jwe `45`; si `45` soti nan rezilta a, peman an depann de lo kote li soti la. |
| Maryaj | Pari sou 2 nimewo 2 chif ansanm. Egzanp: `12-45`. |
| Maryaj gratis | Liy pwomosyonèl gratis sistèm lan ka ajoute selon règ tenansye a. |
| Loto 3 | Pari sou yon nimewo 3 chif. Egzanp: `123`. |
| Loto 4 | Pari sou yon nimewo 4 chif. Egzanp: `1234`. |
| Loto 5 | Pari sou yon nimewo 5 chif. Egzanp: `12345`. |

Yon admin pa kreye jwèt lib. Li aktive, dezaktive, montre, kache, klase oswa
ajiste jwèt Tchalanet sipòte yo.

## Opsyon jwèt

Yon opsyon jwèt se règ ki di kijan sistèm lan dwe verifye pari a apre rezilta a
soti. Kliyan an bay nimewo li yo; opsyon an di ki kalite match ki fè tikè a
genyen.

Opsyon nou sipòte:

| Jwèt | Sa kliyan an bay | Opsyon / règ | Egzanp |
|---|---|---|---|
| Bòlèt | Yon nimewo 2 chif | Premye lo, dezyèm lo, twazyèm lo kòm pozisyon rezilta ki ka peye | Kliyan jwe `45`. Si `45` soti nan premye lo, li touche barèm premye lo; si li soti nan dezyèm oswa twazyèm lo, barèm nan chanje. |
| Maryaj | De nimewo 2 chif | Lòd egzak, ranvèse / doub | Kliyan jwe `12-45`. Lòd egzak mande `12` ak `45` nan menm lòd la. Ranvèse ka pèmèt `45-12` selon règ ki aktive. |
| Maryaj gratis | De nimewo 2 chif sou liy gratis | Menm prensip Maryaj, men kòm pwomosyon | Si règ la aktif, sistèm lan ka ajoute yon liy Maryaj gratis avan machann nan konfime tikè a. |
| Loto 3 | Yon nimewo 3 chif | Egzak, dezòd / box, egzak + pèmitasyon | Kliyan jwe `123`. Egzak mande `123`. Dezòd / box ka aksepte menm chif yo nan lòt lòd, tankou `132` oswa `321`. |
| Loto 4 | Yon nimewo 4 chif | Egzak, dezòd / box, 2 premye chif, 2 dènye chif, egzak + pèmitasyon | Kliyan jwe `1234`. Egzak mande `1234`. 2 premye chif gade `12`; 2 dènye chif gade `34`. |
| Loto 5 | Yon nimewo 5 chif | 1e lo + 2e lo, 1e lo + 3e lo, melanje 1e/2e/3e lo | Kliyan jwe `12345`. Règ la di ak ki lo rezilta sistèm lan dwe konpare nimewo a. |

Gen 2 mòd seleksyon opsyon:

| Mòd | Sans | Egzanp |
|---|---|---|
| Mòd explicite | Machann nan chwazi opsyon an klèman sou POS la. | Machann chwazi yon opsyon tankou `Egzak` oswa `Dezòd / box` si jwèt la mande sa. |
| Mòd implicite | Sistèm lan chwazi opsyon ki pi byen matche ak nimewo a oswa jwèt la. | Pou Maryaj, Loto 3, Loto 4 oswa Loto 5, sistèm lan ka dedwi règ ki aplikab la selon konfigirasyon an. |

Egzak + pèmitasyon disponib pou Loto 3 ak Loto 4, men admin lan dwe aktive li
si li vle machann yo itilize li.

## Odds / barèm

Odds se barèm peman an. Li di konbyen kliyan an ka genyen si pari a genyen.

Egzanp senp:

```text
Miz = 10 G
Odds = 50
Ganyan = 10 G x 50 = 500 G
```

Gen 2 fason prensipal pou peye:

| Tip | Sans |
|---|---|
| Miltiplikasyon miz | Ganyan an kalkile kòm miz x odds. |
| Montan fiks | Ganyan an se yon montan fiks. |

Maryaj gratis itilize yon lojik pwomosyonèl: liy la gratis, men li ka gen yon
montan peman fiks selon règ ki konfigire.

## Barèm / borne / limit

Mo sa yo pa vle di menm bagay.

| Mo | Sans |
|---|---|
| Barèm | Konbyen sistèm nan peye lè yon pari genyen. |
| Borne | Miz minimòm ak miz maksimòm pou yon jwèt. Tenansye a konfigire borne yo lè l ap mete règ biznis li. |
| Limit | Règ ki kontwole risk oswa dwa. Egzanp: pa vann plis pase X sou menm nimewo pou menm tiraj. |

Egzanp diferans:

- Barèm Bòlèt premye lo ka peye 50x.
- Borne jwèt la ka di pou Bòlèt, miz dwe ant 5 G ak 500 G.
- Limit sou nimewo a ka di pa vann plis pase 2,000 G sou nimewo `45` pou menm tiraj.

## Limit

Gen 2 fanmi limit.

### Limit plan

Limit plan soti nan abònman espas kliyan an.

Egzanp:

- konbyen machin POS espas kliyan an ka genyen;
- konbyen itilizatè li ka genyen;
- konbyen tickets pa jou si plan an mete sa.

### Limit vant / risk

Limit vant pwoteje operasyon an.

Egzanp règ ki egziste nan domèn limit yo:

- miz maksimòm pou yon liy;
- kantite liy maksimòm sou yon tikè;
- miz total maksimòm sou yon tikè;
- ekspozisyon maksimòm sou yon seleksyon pou yon tiraj;
- bloke yon seleksyon pou yon tiraj;
- bloke yon kalite pari.

Yon limit ka retounen:

- `ALLOW` - operasyon an pase;
- `WARN` - operasyon an pase men gen avètisman;
- `REQUIRE_APPROVAL` - bezwen apwobasyon;
- `BLOCK` - operasyon an bloke.

## Règ espesyal

Yon règ espesyal se yon eksepsyon ki pase sou règ jeneral tenansye a pou yon
machann, yon machin POS, yon jwèt, yon tiraj oswa yon sitiyasyon byen presi.

Egzanp règ espesyal:

- tenansye a gen komisyon debaz `10%`, men li bay yon machann espesifik `12%`;
- barèm Bòlèt debaz la se `50x / 20x / 10x`, men yon machann gen dwa peye
  `60x / 25x / 15x`;
- admin mete yon limit espesyal pou yon machin POS;
- admin bloke yon nimewo pou yon tiraj;
- admin antre yon rezilta manyèl lè founisè a pa bay rezilta.

Règ pou PDF yo:

> Règ espesyal vle di gen yon règ jeneral, men gen yon eksepsyon ki aplike pou
> yon machann, yon machin POS oswa yon sitiyasyon byen presi.

## Maryaj gratis

Maryaj gratis se yon avantaj / pwomosyon. Lè règ la aktif, sistèm nan ka ajoute
yon liy Maryaj gratis sou preparasyon tikè a. Machann nan wè liy gratis la avan
li konfime vant lan.

Nan sistèm lan:

- Maryaj gratis se yon jwèt ki ka aktive oswa dezaktive;
- tenansye a ka chwazi si Maryaj gratis aktif;
- sou app mobil la, liy gratis yo parèt pandan preparasyon tikè a;
- sou tikè a, liy pwomosyonèl yo make kòm gratis.

## Draw, draw channel, result slot

Tchalanet separe 3 konsèp.

### Result slot

Result slot se sous rezilta pou yon tiraj. Li di ki founisè rezilta sistèm lan
dwe swiv, ki lè rezilta a soti, epi ki règ sistèm lan itilize pou li nimewo yo.

Yon founisè rezilta se eta oswa enstitisyon ki pibliye rezilta bòlèt la. Egzanp:
Nouyòk, Florid, Kalifòni, Michigann.

Li gen:

- yon non oswa kòd referans;
- founisè rezilta a;
- timezone;
- lè tiraj founisè a;
- jou li mache;
- si li aktif;
- si rezilta a vini otomatikman oswa manyèlman;
- règ pou li rezilta founisè a.

Egzanp: New York midi, Florida swa, California midi.

### Draw channel

Draw channel se kanal tiraj espas kliyan yo itilize pou vann. Li konekte ak yon
result slot, men li gen pwòp non komèsyal li, cutoff, ak estati aktif.

Egzanp: yon kanal New York midi ka sèvi pou vann tiraj Ayiti ki baze sou rezilta
New York midi.

### Draw

Draw se yon tiraj konkrè pou yon espas kliyan, yon jou ak yon lè.

Estati prensipal yo:

| Estati | Sans |
|---|---|
| Pwograme | Tiraj la poko ouvè. |
| Ouvè | Machann ka vann. |
| Fèmen | Vant fini, ap tann rezilta oswa règleman. |
| Rezilta antre | Rezilta tiraj la antre. |
| Regle | Kalkil ak règleman fèt. |
| Anile | Tiraj la pa kontinye. |
| Achive | Tiraj la soti nan operasyon aktif. |

## Konbyen nou sipòte kounye a

Nou sipòte kounye a:

- 6 jwèt;
- 13 founisè / eta US: NY, FL, GA, TX, PA, NJ, CA, OH, MI, TN, IL, MO, MN;
- 27 result slots;
- 19 draw channels Haiti.

Kèk founisè gen rezilta otomatik, kèk lòt mande antre manyèl. Nan konfigirasyon
aktyèl la, TN, IL ak MN gen result slots an mòd manyèl.

Sa ka evolye, men PDF yo dwe reflete eta sa a jiskaske konfigirasyon ofisyèl la
chanje.

## Aktive / dezaktive

Gen plizyè nivo aktive / dezaktive:

| Nivo | Efè |
|---|---|
| Jwèt espas kliyan | Jwèt la parèt oswa pa parèt pou espas kliyan an. |
| Opsyon jwèt | Opsyon an parèt oswa pa parèt nan POS. |
| Draw channel | Kanal tiraj la disponib oswa pa disponib pou vann. |
| Draw | Yon tiraj espesifik louvri, fèmen, anile, oswa rezilte. |
| Result slot | Sous rezilta founisè a aktif oswa pa aktif globalman. |

Pou admin espas kliyan an, aksyon nòmal yo se jwèt, opsyon jwèt, draw channel,
draw, limit ak règ espesyal manyèl. Result slot se plis yon konfigirasyon platform.
