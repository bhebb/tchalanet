# Operasyon admin ak machann - vann, kontwole, rapòte

Dokiman sa a esplike kijan Tchalanet dwe itilize apre konfigirasyon an. Li sèvi
kòm baz pou PDF vendeur/admin ak paj èd yo.

## Wòl yo

| Wòl | Travay prensipal |
|---|---|
| Machann | Vann tikè, verifye tikè, gade rapò pa li. |
| Admin espas kliyan | Kontwole machann, tiraj, tikè, rapò, limit ak règ espesyal. |
| Platform / superadmin | Jere katalòg global, founisè rezilta, result slots, infra ak sipò avanse. |

Admin espas kliyan an pa gen vokasyon pou vann tikè chak jou. Li ka gen aksè
pou teste, men operasyon nòmal la se kontwòl.

## Operasyon machann

Machann nan dwe kapab fè 4 aksyon san asistans:

1. konekte sou machin POS la;
2. vann yon tikè;
3. verifye oswa re-enprime yon tikè;
4. gade rapò li.

## Vann yon tikè

Flow aktyèl POS la:

```text
Chwazi tiraj -> chwazi jwèt -> chwazi opsyon si genyen -> antre nimewo
-> antre miz -> prepare -> verifye recap -> konfime -> tikè
```

`Prepare` pa vann tikè a. Li mande sistèm lan kalkile:

- total;
- liy yo;
- limit / avètisman;
- promosyon tankou Maryaj gratis;
- nimewo preparasyon an.

`Confirm` se etap ki kreye tikè a finalman.

## Lè gen erè pandan vant

PDF machann nan dwe bay repons senp:

| Sitiyasyon | Sa machann fè |
|---|---|
| Pa gen tiraj disponib | Tcheke entènèt, rafrechi, rele admin si toujou pa parèt. |
| Prepare bay erè | Pa pran lajan final; li mesaj la; eseye ankò oswa rele admin. |
| Confirm bay erè | Pa re-vann san verifye istorik/tikè; rele admin si dout. |
| Tikè pa enprime | Ale nan tikè yo, verifye si tikè a egziste, re-enprime. |
| Sesyon ekspire | Rekonekte, pa fè nouvo vant anvan ou verifye si tikè anvan an egziste. |
| Jwèt / nimewo pa disponib | Sa ka soti nan limit, draw fèmen, oswa founisè a pa bay lot la. |

Egzanp tèks kliyan pou California midi:

```text
Kalifòni pa pibliye Daily4 pou tiraj midi, se poutèt sa lo2 ak lo3 pa disponib.
```

## Rapò machann

Rapò machann lan limite a aktivite tèminal li. Li pa dwe montre total tout lòt
machann yo.

Metrik rapò yo:

| Mo | Sans |
|---|---|
| Tikè vann | Kantite tikè ki vann. |
| Vant brit | Total miz yo. |
| Ganyan kalkile | Sa sistèm nan kalkile kòm ganyan. |
| Komisyon machann | Pati machann nan. |
| Net estime | Estimasyon apre ganyan kalkile ak komisyon. |

Fòmil rapò a:

```text
Net estime = Vant brit - Ganyan kalkile - Komisyon machann - Frè tenansye
```

Pou machann, PDF la dwe rete senp:

```text
Rapò machann = aktivite machin sa a sèlman.
Rapò admin = vizyon global espas kliyan an.
```

## Operasyon admin

Admin lan dwe kontwole:

- machann / tèminal;
- draw channels;
- tiraj ki louvri;
- tiraj ki fèmen;
- rezilta;
- tickets;
- rapò;
- limit;
- notifications;
- règ espesyal.

## Kontwole machann / tèminal

Admin ka:

- kreye yon tèminal;
- mete komisyon;
- mete PIN inisyal;
- bloke / debloke;
- reset PIN;
- dezaktive;
- wè vant jounen an.

Paj èd admin dwe eksplike:

> Si yon machann pa dwe vann ankò, bloke oswa dezaktive tèminal la avan ou fè lòt
> aksyon.

## Kontwole tiraj

Admin dwe separe:

- draw channel: kanal ki pèmèt vant;
- draw: tiraj konkrè pou yon jou/lè;
- result slot: sous rezilta founisè a.

Operasyon nòmal:

| Aksyon | Efè |
|---|---|
| Aktive draw channel | Li ka jenere / parèt pou vant. |
| Dezaktive draw channel | Li pa dwe parèt pou vant nouvo. |
| Fèmen draw | Pa gen nouvo vant sou tiraj sa. |
| Anile draw | Tiraj la pa kontinye nan flow nòmal. |
| Mete rezilta manyèl | Règ espesyal lè founisè a pa bay rezilta. |

## Kontwole rezilta

Rezilta ka antre otomatikman oswa manyèlman.

| Tip | Sans |
|---|---|
| Otomatik | Sistèm nan jwenn rezilta nan founisè a. |
| Manyèl pwopoze | Admin tenansye a pwopoze rezilta a paske rezilta yo poko rive oswa founisè a pa disponib. |
| Manyèl konfime | Yon operatè Tchalanet / super admin konfime rezilta pwopoze a avan li vin referans final. |
| Règ espesyal | Rezilta oswa konfigirasyon manyèl ki pase sou flow nòmal la. |

Notifications admin dwe klè:

- rezilta auto aplike;
- rezilta manyèl aplike;
- admin pwopoze yon rezilta;
- operatè Tchalanet konfime yon rezilta;
- founisè a pa bay rezilta;
- règ espesyal fèt;
- draw rete san rezilta apre lè atann.

## Kontwole tickets

Admin ka itilize tickets pou:

- jwenn yon tikè;
- verifye estati;
- wè tèminal ki vann li;
- wè tiraj ak jwèt;
- wè si li promotional;
- ede machann lè enprimant oswa rezo gen pwoblèm.

Admin pa dwe modifye tikè fasil san tras kontwòl oswa règ espesyal. Tikè se
prèv operasyon an.

## Rapò admin

Rapò admin se referans final pou espas kliyan an.

Metrik rapò admin yo:

| Mo | Sans |
|---|---|
| Vant brit | Total miz yo. |
| Ganyan kalkile | Montan sistèm lan estime ki dwe peye sou tikè ki genyen yo. |
| Ganyan peye | Montan ganyan ki deja peye. |
| Komisyon machann | Pati machann nan. |
| Frè kliyan | Frè ki aplike bò kliyan / achtè. |
| Frè machann | Frè ki aplike bò machann nan. |
| Frè tenansye | Frè ki rete sou espas kliyan an. |
| Frè retire | Frè sistèm lan retire pou pwomosyon oswa ajisteman. |
| Liy pwomosyon | Liy gratis oswa liy espesyal, tankou Maryaj gratis. |
| Net estime | Pozisyon estime apre ganyan kalkile ak frè yo. |
| Net baz peye | Pozisyon selon ganyan ki deja peye. |

Fòmil aktyèl:

```text
Net estime = Vant brit - Ganyan kalkile - Komisyon machann - Frè tenansye
Net baz peye = Vant brit - Ganyan peye - Komisyon machann - Frè tenansye
```

PDF admin lan dwe eksplike diferans lan:

- `Net estime` itil avan tout ganyan yo peye.
- `Net baz peye` montre pozisyon selon peman ki deja fèt.

## Reconciliation ak archive

Reconciliation sèvi pou konpare projection analytics yo ak done sous yo.

Archive sèvi pou retire done ansyen nan operasyon aktif pandan yo rete
konservab selon règleman.

Pou kounye a, dokiman kliyan yo dwe di:

> Rapò yo soti nan projection analytics. Si gen dout, admin dwe itilize
> reconciliation / support avan li pran desizyon final sou diferans la.

## Switch-off operasyonèl

Si gen atak, erè grav, oswa risk finansye:

1. fèmen vant sou draw/channel ki afekte a;
2. bloke tèminal si pwoblèm nan soti nan yon machann;
3. mete maintenance / switch-off si sistèm antye an risk;
4. verifye tickets ak rapò;
5. re-ouvri sèlman apre smoke test.
