# Kenya Pure Geodetic Grid Specification (0.5° Lat/Long Lines)

> **User Defined Edge Values:**  
> &bull; **West:** `34.0° E`  
> &bull; **South:** `-5.0° S (5°S)`  
> &bull; **East:** `42.0° E`  
> &bull; **North:** `8.0° N`  
> **Grid Step:** Pure **0.5°** Latitude & Longitude Lines  
> **Grid Dimensions:** **16 Columns** × **26 Rows** = **416 Pure Grid Cells**  
> **Coverage:** **204 Kenya Sovereign Land Cells** + **212 Regional Buffer Cells** (Uganda, Tanzania, Southern Ethiopia up to 8°N, Somalia, Indian Ocean)  
> **Database:** `terrain_grid/kenya_terrain_stiles.sqlite` (and root `stiles.db`)  
> **Master Map:** [`terrain_grid/kenya_grid_map.html`](terrain_grid/kenya_grid_map.html)  
> **Geodetic Realism:** *"Square is just estimates / a guide. Even when we draw these grids, they aren't actually perfectly square grids on the ellipsoid."*

---

## 1. Architectural Principles

1. **Pure Grid Cells, NOT Contours**:
   - The map and system do not use elevation contour lines.
   - It is structured as pure, discrete geometric geodetic grid cells spaced at regular 0.5° intervals along lines of latitude and longitude.
2. **"Square" as an Estimation Guide**:
   - On the spherical/ellipsoidal Earth (WGS84 EPSG:4326), lines of latitude maintain an isotropic distance of ~111.13 km per degree (~55.57 km per 0.5°).
   - Lines of longitude converge as distance from the equator increases: 111.32 * cos(lat) km.
   - Thus, cells are spherical trapezoids that are close to square at the equator, and slightly narrower at higher latitudes. The term "square" is an engineering estimation guide.
3. **Decoupling Roads from Terrain**:
   - Roads are continuous 1D directed spline corridors with sub-meter cross sections and physical banking.
   - Terrain is a broad 2D heightfield partitioned into discrete super-tiles (STiles) defined by integer grid lines at 0.5° steps.
4. **All 416 Starting Points Explicitly Documented**:
   - Every grid cell is mathematically and programmatically referenced by its **Starting Point (Lat_start, Lon_start)**, which represents the South-West corner datum of that cell.

---

## 2. Grid Geometry & Edge Dimensions

* **Longitude Span**: 42.0° - 34.0° = 8.0°
  * Columns: 8.0° / 0.5° = 16 columns (c in [0 .. 15])
  * Grid lines of Longitude: 34.0°, 34.5°, 35.0°, 35.5°, 36.0°, 36.5°, 37.0°, 37.5°, 38.0°, 38.5°, 39.0°, 39.5°, 40.0°, 40.5°, 41.0°, 41.5°, 42.0°
* **Latitude Span**: 8.0° - (-5.0°) = 13.0°
  * Rows: 13.0° / 0.5° = 26 rows (r in [0 .. 25])
  * Grid lines of Latitude: -5.0°, -4.5°, -4.0°, -3.5°, -3.0°, -2.5°, -2.0°, -1.5°, -1.0°, -0.5°, 0.0° (Equator), 0.5°, 1.0°, 1.5°, 2.0°, 2.5°, 3.0°, 3.5°, 4.0°, 4.5°, 5.0°, 5.5°, 6.0°, 6.5°, 7.0°, 7.5°, 8.0°
* **Mathematical Formula for Any Cell (r, c)**:
  - Lat_start = -5.0 + r * 0.5
  - Lon_start = 34.0 + c * 0.5
  - Lat_end = Lat_start + 0.5
  - Lon_end = Lon_start + 0.5
  - Lat_center = Lat_start + 0.25, Lon_center = Lon_start + 0.25

---

## 3. SQLite Database Schema (`kenya_terrain_stiles.sqlite` / `stiles.db`)

```sql
CREATE TABLE stiles_metadata (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE TABLE stiles_grid (
    tile_id INTEGER PRIMARY KEY,
    tile_code TEXT UNIQUE NOT NULL,      -- e.g. 'STILE_R07_C05'
    tile_name TEXT NOT NULL,
    row_idx INTEGER NOT NULL,            -- 0 to 25 (South to North)
    col_idx INTEGER NOT NULL,            -- 0 to 15 (West to East)
    start_lat REAL NOT NULL,             -- SW Latitude starting point
    start_lon REAL NOT NULL,             -- SW Longitude starting point
    end_lat REAL NOT NULL,               -- NE Latitude ending point
    end_lon REAL NOT NULL,               -- NE Longitude ending point
    center_lat REAL NOT NULL,
    center_lon REAL NOT NULL,
    metric_x_min REAL NOT NULL,
    metric_x_max REAL NOT NULL,
    metric_y_min REAL NOT NULL,
    metric_y_max REAL NOT NULL,
    region_type TEXT NOT NULL,
    is_kenya_territory INTEGER NOT NULL, -- 1 = Kenya land, 0 = Buffer
    elev_min_m REAL NOT NULL,
    elev_max_m REAL NOT NULL,
    elev_mean_m REAL NOT NULL,
    primary_biome TEXT NOT NULL,
    countries_csv TEXT NOT NULL,
    neighbor_north TEXT,
    neighbor_south TEXT,
    neighbor_east TEXT,
    neighbor_west TEXT,
    html_filename TEXT NOT NULL,
    binary_filename TEXT NOT NULL
);

CREATE INDEX idx_stiles_start ON stiles_grid(start_lat, start_lon);
CREATE INDEX idx_stiles_row_col ON stiles_grid(row_idx, col_idx);
CREATE INDEX idx_stiles_kenya ON stiles_grid(is_kenya_territory);
```

### Fast Spatial Query by GPS Coordinate
```sql
-- Find cell for Nairobi (-1.286° S, 36.817° E):
SELECT tile_code, start_lat, start_lon, end_lat, end_lon, tile_name, html_filename
FROM stiles_grid
WHERE -1.286 >= start_lat AND -1.286 < end_lat
  AND 36.817 >= start_lon AND 36.817 < end_lon;
-- Result: STILE_R07_C05 (Starting Point: -1.5° Lat, 36.5° Lon)
```

---

## 4. Complete Catalog of All 416 Starting Points

| Tile Code | Matrix (R, C) | Starting Point (SW) | Ending Point (NE) | Elevation Range | Regional Classification | Sovereignty | HTML Definition |
|---|---|---|---|---|---|---|---|
| `STILE_R00_C00` | R00 C00 | **(-5.0°, +34.0°)** | (-4.5°, +34.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r00_c00.html`](terrain_grid/stiles_html/stile_r00_c00.html) |
| `STILE_R00_C01` | R00 C01 | **(-5.0°, +34.5°)** | (-4.5°, +35.0°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r00_c01.html`](terrain_grid/stiles_html/stile_r00_c01.html) |
| `STILE_R00_C02` | R00 C02 | **(-5.0°, +35.0°)** | (-4.5°, +35.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r00_c02.html`](terrain_grid/stiles_html/stile_r00_c02.html) |
| `STILE_R00_C03` | R00 C03 | **(-5.0°, +35.5°)** | (-4.5°, +36.0°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r00_c03.html`](terrain_grid/stiles_html/stile_r00_c03.html) |
| `STILE_R00_C04` | R00 C04 | **(-5.0°, +36.0°)** | (-4.5°, +36.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r00_c04.html`](terrain_grid/stiles_html/stile_r00_c04.html) |
| `STILE_R00_C05` | R00 C05 | **(-5.0°, +36.5°)** | (-4.5°, +37.0°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r00_c05.html`](terrain_grid/stiles_html/stile_r00_c05.html) |
| `STILE_R00_C06` | R00 C06 | **(-5.0°, +37.0°)** | (-4.5°, +37.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r00_c06.html`](terrain_grid/stiles_html/stile_r00_c06.html) |
| `STILE_R00_C07` | R00 C07 | **(-5.0°, +37.5°)** | (-4.5°, +38.0°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r00_c07.html`](terrain_grid/stiles_html/stile_r00_c07.html) |
| `STILE_R00_C08` | R00 C08 | **(-5.0°, +38.0°)** | (-4.5°, +38.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r00_c08.html`](terrain_grid/stiles_html/stile_r00_c08.html) |
| `STILE_R00_C09` | R00 C09 | **(-5.0°, +38.5°)** | (-4.5°, +39.0°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r00_c09.html`](terrain_grid/stiles_html/stile_r00_c09.html) |
| `STILE_R00_C10` | R00 C10 | **(-5.0°, +39.0°)** | (-4.5°, +39.5°) | 0–310 m | Kenya — Swahili Coastal Strip & Marine Shelf | Kenya | [`stile_r00_c10.html`](terrain_grid/stiles_html/stile_r00_c10.html) |
| `STILE_R00_C11` | R00 C11 | **(-5.0°, +39.5°)** | (-4.5°, +40.0°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r00_c11.html`](terrain_grid/stiles_html/stile_r00_c11.html) |
| `STILE_R00_C12` | R00 C12 | **(-5.0°, +40.0°)** | (-4.5°, +40.5°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r00_c12.html`](terrain_grid/stiles_html/stile_r00_c12.html) |
| `STILE_R00_C13` | R00 C13 | **(-5.0°, +40.5°)** | (-4.5°, +41.0°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r00_c13.html`](terrain_grid/stiles_html/stile_r00_c13.html) |
| `STILE_R00_C14` | R00 C14 | **(-5.0°, +41.0°)** | (-4.5°, +41.5°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r00_c14.html`](terrain_grid/stiles_html/stile_r00_c14.html) |
| `STILE_R00_C15` | R00 C15 | **(-5.0°, +41.5°)** | (-4.5°, +42.0°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r00_c15.html`](terrain_grid/stiles_html/stile_r00_c15.html) |
| `STILE_R01_C00` | R01 C00 | **(-4.5°, +34.0°)** | (-4.0°, +34.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r01_c00.html`](terrain_grid/stiles_html/stile_r01_c00.html) |
| `STILE_R01_C01` | R01 C01 | **(-4.5°, +34.5°)** | (-4.0°, +35.0°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r01_c01.html`](terrain_grid/stiles_html/stile_r01_c01.html) |
| `STILE_R01_C02` | R01 C02 | **(-4.5°, +35.0°)** | (-4.0°, +35.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r01_c02.html`](terrain_grid/stiles_html/stile_r01_c02.html) |
| `STILE_R01_C03` | R01 C03 | **(-4.5°, +35.5°)** | (-4.0°, +36.0°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r01_c03.html`](terrain_grid/stiles_html/stile_r01_c03.html) |
| `STILE_R01_C04` | R01 C04 | **(-4.5°, +36.0°)** | (-4.0°, +36.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r01_c04.html`](terrain_grid/stiles_html/stile_r01_c04.html) |
| `STILE_R01_C05` | R01 C05 | **(-4.5°, +36.5°)** | (-4.0°, +37.0°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r01_c05.html`](terrain_grid/stiles_html/stile_r01_c05.html) |
| `STILE_R01_C06` | R01 C06 | **(-4.5°, +37.0°)** | (-4.0°, +37.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r01_c06.html`](terrain_grid/stiles_html/stile_r01_c06.html) |
| `STILE_R01_C07` | R01 C07 | **(-4.5°, +37.5°)** | (-4.0°, +38.0°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r01_c07.html`](terrain_grid/stiles_html/stile_r01_c07.html) |
| `STILE_R01_C08` | R01 C08 | **(-4.5°, +38.0°)** | (-4.0°, +38.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r01_c08.html`](terrain_grid/stiles_html/stile_r01_c08.html) |
| `STILE_R01_C09` | R01 C09 | **(-4.5°, +38.5°)** | (-4.0°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r01_c09.html`](terrain_grid/stiles_html/stile_r01_c09.html) |
| `STILE_R01_C10` | R01 C10 | **(-4.5°, +39.0°)** | (-4.0°, +39.5°) | 0–310 m | Kenya — Swahili Coastal Strip & Marine Shelf | Kenya | [`stile_r01_c10.html`](terrain_grid/stiles_html/stile_r01_c10.html) |
| `STILE_R01_C11` | R01 C11 | **(-4.5°, +39.5°)** | (-4.0°, +40.0°) | 0–310 m | Kenya — Swahili Coastal Strip & Marine Shelf | Kenya | [`stile_r01_c11.html`](terrain_grid/stiles_html/stile_r01_c11.html) |
| `STILE_R01_C12` | R01 C12 | **(-4.5°, +40.0°)** | (-4.0°, +40.5°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r01_c12.html`](terrain_grid/stiles_html/stile_r01_c12.html) |
| `STILE_R01_C13` | R01 C13 | **(-4.5°, +40.5°)** | (-4.0°, +41.0°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r01_c13.html`](terrain_grid/stiles_html/stile_r01_c13.html) |
| `STILE_R01_C14` | R01 C14 | **(-4.5°, +41.0°)** | (-4.0°, +41.5°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r01_c14.html`](terrain_grid/stiles_html/stile_r01_c14.html) |
| `STILE_R01_C15` | R01 C15 | **(-4.5°, +41.5°)** | (-4.0°, +42.0°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r01_c15.html`](terrain_grid/stiles_html/stile_r01_c15.html) |
| `STILE_R02_C00` | R02 C00 | **(-4.0°, +34.0°)** | (-3.5°, +34.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r02_c00.html`](terrain_grid/stiles_html/stile_r02_c00.html) |
| `STILE_R02_C01` | R02 C01 | **(-4.0°, +34.5°)** | (-3.5°, +35.0°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r02_c01.html`](terrain_grid/stiles_html/stile_r02_c01.html) |
| `STILE_R02_C02` | R02 C02 | **(-4.0°, +35.0°)** | (-3.5°, +35.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r02_c02.html`](terrain_grid/stiles_html/stile_r02_c02.html) |
| `STILE_R02_C03` | R02 C03 | **(-4.0°, +35.5°)** | (-3.5°, +36.0°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r02_c03.html`](terrain_grid/stiles_html/stile_r02_c03.html) |
| `STILE_R02_C04` | R02 C04 | **(-4.0°, +36.0°)** | (-3.5°, +36.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r02_c04.html`](terrain_grid/stiles_html/stile_r02_c04.html) |
| `STILE_R02_C05` | R02 C05 | **(-4.0°, +36.5°)** | (-3.5°, +37.0°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r02_c05.html`](terrain_grid/stiles_html/stile_r02_c05.html) |
| `STILE_R02_C06` | R02 C06 | **(-4.0°, +37.0°)** | (-3.5°, +37.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r02_c06.html`](terrain_grid/stiles_html/stile_r02_c06.html) |
| `STILE_R02_C07` | R02 C07 | **(-4.0°, +37.5°)** | (-3.5°, +38.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r02_c07.html`](terrain_grid/stiles_html/stile_r02_c07.html) |
| `STILE_R02_C08` | R02 C08 | **(-4.0°, +38.0°)** | (-3.5°, +38.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r02_c08.html`](terrain_grid/stiles_html/stile_r02_c08.html) |
| `STILE_R02_C09` | R02 C09 | **(-4.0°, +38.5°)** | (-3.5°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r02_c09.html`](terrain_grid/stiles_html/stile_r02_c09.html) |
| `STILE_R02_C10` | R02 C10 | **(-4.0°, +39.0°)** | (-3.5°, +39.5°) | 0–310 m | Kenya — Swahili Coastal Strip & Marine Shelf | Kenya | [`stile_r02_c10.html`](terrain_grid/stiles_html/stile_r02_c10.html) |
| `STILE_R02_C11` | R02 C11 | **(-4.0°, +39.5°)** | (-3.5°, +40.0°) | 0–310 m | Kenya — Swahili Coastal Strip & Marine Shelf | Kenya | [`stile_r02_c11.html`](terrain_grid/stiles_html/stile_r02_c11.html) |
| `STILE_R02_C12` | R02 C12 | **(-4.0°, +40.0°)** | (-3.5°, +40.5°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r02_c12.html`](terrain_grid/stiles_html/stile_r02_c12.html) |
| `STILE_R02_C13` | R02 C13 | **(-4.0°, +40.5°)** | (-3.5°, +41.0°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r02_c13.html`](terrain_grid/stiles_html/stile_r02_c13.html) |
| `STILE_R02_C14` | R02 C14 | **(-4.0°, +41.0°)** | (-3.5°, +41.5°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r02_c14.html`](terrain_grid/stiles_html/stile_r02_c14.html) |
| `STILE_R02_C15` | R02 C15 | **(-4.0°, +41.5°)** | (-3.5°, +42.0°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r02_c15.html`](terrain_grid/stiles_html/stile_r02_c15.html) |
| `STILE_R03_C00` | R03 C00 | **(-3.5°, +34.0°)** | (-3.0°, +34.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r03_c00.html`](terrain_grid/stiles_html/stile_r03_c00.html) |
| `STILE_R03_C01` | R03 C01 | **(-3.5°, +34.5°)** | (-3.0°, +35.0°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r03_c01.html`](terrain_grid/stiles_html/stile_r03_c01.html) |
| `STILE_R03_C02` | R03 C02 | **(-3.5°, +35.0°)** | (-3.0°, +35.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r03_c02.html`](terrain_grid/stiles_html/stile_r03_c02.html) |
| `STILE_R03_C03` | R03 C03 | **(-3.5°, +35.5°)** | (-3.0°, +36.0°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r03_c03.html`](terrain_grid/stiles_html/stile_r03_c03.html) |
| `STILE_R03_C04` | R03 C04 | **(-3.5°, +36.0°)** | (-3.0°, +36.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r03_c04.html`](terrain_grid/stiles_html/stile_r03_c04.html) |
| `STILE_R03_C05` | R03 C05 | **(-3.5°, +36.5°)** | (-3.0°, +37.0°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r03_c05.html`](terrain_grid/stiles_html/stile_r03_c05.html) |
| `STILE_R03_C06` | R03 C06 | **(-3.5°, +37.0°)** | (-3.0°, +37.5°) | 700–2400 m | Border Zone — Northern Tanzania & Kilimanjaro Steppe | Buffer | [`stile_r03_c06.html`](terrain_grid/stiles_html/stile_r03_c06.html) |
| `STILE_R03_C07` | R03 C07 | **(-3.5°, +37.5°)** | (-3.0°, +38.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r03_c07.html`](terrain_grid/stiles_html/stile_r03_c07.html) |
| `STILE_R03_C08` | R03 C08 | **(-3.5°, +38.0°)** | (-3.0°, +38.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r03_c08.html`](terrain_grid/stiles_html/stile_r03_c08.html) |
| `STILE_R03_C09` | R03 C09 | **(-3.5°, +38.5°)** | (-3.0°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r03_c09.html`](terrain_grid/stiles_html/stile_r03_c09.html) |
| `STILE_R03_C10` | R03 C10 | **(-3.5°, +39.0°)** | (-3.0°, +39.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r03_c10.html`](terrain_grid/stiles_html/stile_r03_c10.html) |
| `STILE_R03_C11` | R03 C11 | **(-3.5°, +39.5°)** | (-3.0°, +40.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r03_c11.html`](terrain_grid/stiles_html/stile_r03_c11.html) |
| `STILE_R03_C12` | R03 C12 | **(-3.5°, +40.0°)** | (-3.0°, +40.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r03_c12.html`](terrain_grid/stiles_html/stile_r03_c12.html) |
| `STILE_R03_C13` | R03 C13 | **(-3.5°, +40.5°)** | (-3.0°, +41.0°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r03_c13.html`](terrain_grid/stiles_html/stile_r03_c13.html) |
| `STILE_R03_C14` | R03 C14 | **(-3.5°, +41.0°)** | (-3.0°, +41.5°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r03_c14.html`](terrain_grid/stiles_html/stile_r03_c14.html) |
| `STILE_R03_C15` | R03 C15 | **(-3.5°, +41.5°)** | (-3.0°, +42.0°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r03_c15.html`](terrain_grid/stiles_html/stile_r03_c15.html) |
| `STILE_R04_C00` | R04 C00 | **(-3.0°, +34.0°)** | (-2.5°, +34.5°) | 1135–1900 m | Border Zone — Eastern Uganda & Lake Victoria Shore | Buffer | [`stile_r04_c00.html`](terrain_grid/stiles_html/stile_r04_c00.html) |
| `STILE_R04_C01` | R04 C01 | **(-3.0°, +34.5°)** | (-2.5°, +35.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r04_c01.html`](terrain_grid/stiles_html/stile_r04_c01.html) |
| `STILE_R04_C02` | R04 C02 | **(-3.0°, +35.0°)** | (-2.5°, +35.5°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r04_c02.html`](terrain_grid/stiles_html/stile_r04_c02.html) |
| `STILE_R04_C03` | R04 C03 | **(-3.0°, +35.5°)** | (-2.5°, +36.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r04_c03.html`](terrain_grid/stiles_html/stile_r04_c03.html) |
| `STILE_R04_C04` | R04 C04 | **(-3.0°, +36.0°)** | (-2.5°, +36.5°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r04_c04.html`](terrain_grid/stiles_html/stile_r04_c04.html) |
| `STILE_R04_C05` | R04 C05 | **(-3.0°, +36.5°)** | (-2.5°, +37.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r04_c05.html`](terrain_grid/stiles_html/stile_r04_c05.html) |
| `STILE_R04_C06` | R04 C06 | **(-3.0°, +37.0°)** | (-2.5°, +37.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r04_c06.html`](terrain_grid/stiles_html/stile_r04_c06.html) |
| `STILE_R04_C07` | R04 C07 | **(-3.0°, +37.5°)** | (-2.5°, +38.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r04_c07.html`](terrain_grid/stiles_html/stile_r04_c07.html) |
| `STILE_R04_C08` | R04 C08 | **(-3.0°, +38.0°)** | (-2.5°, +38.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r04_c08.html`](terrain_grid/stiles_html/stile_r04_c08.html) |
| `STILE_R04_C09` | R04 C09 | **(-3.0°, +38.5°)** | (-2.5°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r04_c09.html`](terrain_grid/stiles_html/stile_r04_c09.html) |
| `STILE_R04_C10` | R04 C10 | **(-3.0°, +39.0°)** | (-2.5°, +39.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r04_c10.html`](terrain_grid/stiles_html/stile_r04_c10.html) |
| `STILE_R04_C11` | R04 C11 | **(-3.0°, +39.5°)** | (-2.5°, +40.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r04_c11.html`](terrain_grid/stiles_html/stile_r04_c11.html) |
| `STILE_R04_C12` | R04 C12 | **(-3.0°, +40.0°)** | (-2.5°, +40.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r04_c12.html`](terrain_grid/stiles_html/stile_r04_c12.html) |
| `STILE_R04_C13` | R04 C13 | **(-3.0°, +40.5°)** | (-2.5°, +41.0°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r04_c13.html`](terrain_grid/stiles_html/stile_r04_c13.html) |
| `STILE_R04_C14` | R04 C14 | **(-3.0°, +41.0°)** | (-2.5°, +41.5°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r04_c14.html`](terrain_grid/stiles_html/stile_r04_c14.html) |
| `STILE_R04_C15` | R04 C15 | **(-3.0°, +41.5°)** | (-2.5°, +42.0°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r04_c15.html`](terrain_grid/stiles_html/stile_r04_c15.html) |
| `STILE_R05_C00` | R05 C00 | **(-2.5°, +34.0°)** | (-2.0°, +34.5°) | 1135–1900 m | Border Zone — Eastern Uganda & Lake Victoria Shore | Buffer | [`stile_r05_c00.html`](terrain_grid/stiles_html/stile_r05_c00.html) |
| `STILE_R05_C01` | R05 C01 | **(-2.5°, +34.5°)** | (-2.0°, +35.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r05_c01.html`](terrain_grid/stiles_html/stile_r05_c01.html) |
| `STILE_R05_C02` | R05 C02 | **(-2.5°, +35.0°)** | (-2.0°, +35.5°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r05_c02.html`](terrain_grid/stiles_html/stile_r05_c02.html) |
| `STILE_R05_C03` | R05 C03 | **(-2.5°, +35.5°)** | (-2.0°, +36.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r05_c03.html`](terrain_grid/stiles_html/stile_r05_c03.html) |
| `STILE_R05_C04` | R05 C04 | **(-2.5°, +36.0°)** | (-2.0°, +36.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r05_c04.html`](terrain_grid/stiles_html/stile_r05_c04.html) |
| `STILE_R05_C05` | R05 C05 | **(-2.5°, +36.5°)** | (-2.0°, +37.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r05_c05.html`](terrain_grid/stiles_html/stile_r05_c05.html) |
| `STILE_R05_C06` | R05 C06 | **(-2.5°, +37.0°)** | (-2.0°, +37.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r05_c06.html`](terrain_grid/stiles_html/stile_r05_c06.html) |
| `STILE_R05_C07` | R05 C07 | **(-2.5°, +37.5°)** | (-2.0°, +38.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r05_c07.html`](terrain_grid/stiles_html/stile_r05_c07.html) |
| `STILE_R05_C08` | R05 C08 | **(-2.5°, +38.0°)** | (-2.0°, +38.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r05_c08.html`](terrain_grid/stiles_html/stile_r05_c08.html) |
| `STILE_R05_C09` | R05 C09 | **(-2.5°, +38.5°)** | (-2.0°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r05_c09.html`](terrain_grid/stiles_html/stile_r05_c09.html) |
| `STILE_R05_C10` | R05 C10 | **(-2.5°, +39.0°)** | (-2.0°, +39.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r05_c10.html`](terrain_grid/stiles_html/stile_r05_c10.html) |
| `STILE_R05_C11` | R05 C11 | **(-2.5°, +39.5°)** | (-2.0°, +40.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r05_c11.html`](terrain_grid/stiles_html/stile_r05_c11.html) |
| `STILE_R05_C12` | R05 C12 | **(-2.5°, +40.0°)** | (-2.0°, +40.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r05_c12.html`](terrain_grid/stiles_html/stile_r05_c12.html) |
| `STILE_R05_C13` | R05 C13 | **(-2.5°, +40.5°)** | (-2.0°, +41.0°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Kenya | [`stile_r05_c13.html`](terrain_grid/stiles_html/stile_r05_c13.html) |
| `STILE_R05_C14` | R05 C14 | **(-2.5°, +41.0°)** | (-2.0°, +41.5°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r05_c14.html`](terrain_grid/stiles_html/stile_r05_c14.html) |
| `STILE_R05_C15` | R05 C15 | **(-2.5°, +41.5°)** | (-2.0°, +42.0°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r05_c15.html`](terrain_grid/stiles_html/stile_r05_c15.html) |
| `STILE_R06_C00` | R06 C00 | **(-2.0°, +34.0°)** | (-1.5°, +34.5°) | 1135–1900 m | Border Zone — Eastern Uganda & Lake Victoria Shore | Buffer | [`stile_r06_c00.html`](terrain_grid/stiles_html/stile_r06_c00.html) |
| `STILE_R06_C01` | R06 C01 | **(-2.0°, +34.5°)** | (-1.5°, +35.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r06_c01.html`](terrain_grid/stiles_html/stile_r06_c01.html) |
| `STILE_R06_C02` | R06 C02 | **(-2.0°, +35.0°)** | (-1.5°, +35.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r06_c02.html`](terrain_grid/stiles_html/stile_r06_c02.html) |
| `STILE_R06_C03` | R06 C03 | **(-2.0°, +35.5°)** | (-1.5°, +36.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r06_c03.html`](terrain_grid/stiles_html/stile_r06_c03.html) |
| `STILE_R06_C04` | R06 C04 | **(-2.0°, +36.0°)** | (-1.5°, +36.5°) | 588–2029 m | Kenya — Great Rift Valley (Magadi Rd Area / Nairobi) | Kenya | [`stile_r06_c04.html`](terrain_grid/stiles_html/stile_r06_c04.html) |
| `STILE_R06_C05` | R06 C05 | **(-2.0°, +36.5°)** | (-1.5°, +37.0°) | 588–2029 m | Kenya — Great Rift Valley (Magadi Rd Area / Nairobi) | Kenya | [`stile_r06_c05.html`](terrain_grid/stiles_html/stile_r06_c05.html) |
| `STILE_R06_C06` | R06 C06 | **(-2.0°, +37.0°)** | (-1.5°, +37.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r06_c06.html`](terrain_grid/stiles_html/stile_r06_c06.html) |
| `STILE_R06_C07` | R06 C07 | **(-2.0°, +37.5°)** | (-1.5°, +38.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r06_c07.html`](terrain_grid/stiles_html/stile_r06_c07.html) |
| `STILE_R06_C08` | R06 C08 | **(-2.0°, +38.0°)** | (-1.5°, +38.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r06_c08.html`](terrain_grid/stiles_html/stile_r06_c08.html) |
| `STILE_R06_C09` | R06 C09 | **(-2.0°, +38.5°)** | (-1.5°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r06_c09.html`](terrain_grid/stiles_html/stile_r06_c09.html) |
| `STILE_R06_C10` | R06 C10 | **(-2.0°, +39.0°)** | (-1.5°, +39.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r06_c10.html`](terrain_grid/stiles_html/stile_r06_c10.html) |
| `STILE_R06_C11` | R06 C11 | **(-2.0°, +39.5°)** | (-1.5°, +40.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r06_c11.html`](terrain_grid/stiles_html/stile_r06_c11.html) |
| `STILE_R06_C12` | R06 C12 | **(-2.0°, +40.0°)** | (-1.5°, +40.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r06_c12.html`](terrain_grid/stiles_html/stile_r06_c12.html) |
| `STILE_R06_C13` | R06 C13 | **(-2.0°, +40.5°)** | (-1.5°, +41.0°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Kenya | [`stile_r06_c13.html`](terrain_grid/stiles_html/stile_r06_c13.html) |
| `STILE_R06_C14` | R06 C14 | **(-2.0°, +41.0°)** | (-1.5°, +41.5°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Kenya | [`stile_r06_c14.html`](terrain_grid/stiles_html/stile_r06_c14.html) |
| `STILE_R06_C15` | R06 C15 | **(-2.0°, +41.5°)** | (-1.5°, +42.0°) | -2200–0 m | Maritime — Indian Ocean Continental Shelf | Buffer | [`stile_r06_c15.html`](terrain_grid/stiles_html/stile_r06_c15.html) |
| `STILE_R07_C00` | R07 C00 | **(-1.5°, +34.0°)** | (-1.0°, +34.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r07_c00.html`](terrain_grid/stiles_html/stile_r07_c00.html) |
| `STILE_R07_C01` | R07 C01 | **(-1.5°, +34.5°)** | (-1.0°, +35.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r07_c01.html`](terrain_grid/stiles_html/stile_r07_c01.html) |
| `STILE_R07_C02` | R07 C02 | **(-1.5°, +35.0°)** | (-1.0°, +35.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r07_c02.html`](terrain_grid/stiles_html/stile_r07_c02.html) |
| `STILE_R07_C03` | R07 C03 | **(-1.5°, +35.5°)** | (-1.0°, +36.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r07_c03.html`](terrain_grid/stiles_html/stile_r07_c03.html) |
| `STILE_R07_C04` | R07 C04 | **(-1.5°, +36.0°)** | (-1.0°, +36.5°) | 588–2029 m | Kenya — Great Rift Valley (Magadi Rd Area / Nairobi) | Kenya | [`stile_r07_c04.html`](terrain_grid/stiles_html/stile_r07_c04.html) |
| `STILE_R07_C05` | R07 C05 | **(-1.5°, +36.5°)** | (-1.0°, +37.0°) | 588–2029 m | Kenya — Great Rift Valley (Magadi Rd Area / Nairobi) | Kenya | [`stile_r07_c05.html`](terrain_grid/stiles_html/stile_r07_c05.html) |
| `STILE_R07_C06` | R07 C06 | **(-1.5°, +37.0°)** | (-1.0°, +37.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r07_c06.html`](terrain_grid/stiles_html/stile_r07_c06.html) |
| `STILE_R07_C07` | R07 C07 | **(-1.5°, +37.5°)** | (-1.0°, +38.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r07_c07.html`](terrain_grid/stiles_html/stile_r07_c07.html) |
| `STILE_R07_C08` | R07 C08 | **(-1.5°, +38.0°)** | (-1.0°, +38.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r07_c08.html`](terrain_grid/stiles_html/stile_r07_c08.html) |
| `STILE_R07_C09` | R07 C09 | **(-1.5°, +38.5°)** | (-1.0°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r07_c09.html`](terrain_grid/stiles_html/stile_r07_c09.html) |
| `STILE_R07_C10` | R07 C10 | **(-1.5°, +39.0°)** | (-1.0°, +39.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r07_c10.html`](terrain_grid/stiles_html/stile_r07_c10.html) |
| `STILE_R07_C11` | R07 C11 | **(-1.5°, +39.5°)** | (-1.0°, +40.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r07_c11.html`](terrain_grid/stiles_html/stile_r07_c11.html) |
| `STILE_R07_C12` | R07 C12 | **(-1.5°, +40.0°)** | (-1.0°, +40.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r07_c12.html`](terrain_grid/stiles_html/stile_r07_c12.html) |
| `STILE_R07_C13` | R07 C13 | **(-1.5°, +40.5°)** | (-1.0°, +41.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r07_c13.html`](terrain_grid/stiles_html/stile_r07_c13.html) |
| `STILE_R07_C14` | R07 C14 | **(-1.5°, +41.0°)** | (-1.0°, +41.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r07_c14.html`](terrain_grid/stiles_html/stile_r07_c14.html) |
| `STILE_R07_C15` | R07 C15 | **(-1.5°, +41.5°)** | (-1.0°, +42.0°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r07_c15.html`](terrain_grid/stiles_html/stile_r07_c15.html) |
| `STILE_R08_C00` | R08 C00 | **(-1.0°, +34.0°)** | (-0.5°, +34.5°) | 1135–1750 m | Kenya — Lake Victoria Basin & Winam Gulf | Kenya | [`stile_r08_c00.html`](terrain_grid/stiles_html/stile_r08_c00.html) |
| `STILE_R08_C01` | R08 C01 | **(-1.0°, +34.5°)** | (-0.5°, +35.0°) | 1135–1750 m | Kenya — Lake Victoria Basin & Winam Gulf | Kenya | [`stile_r08_c01.html`](terrain_grid/stiles_html/stile_r08_c01.html) |
| `STILE_R08_C02` | R08 C02 | **(-1.0°, +35.0°)** | (-0.5°, +35.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r08_c02.html`](terrain_grid/stiles_html/stile_r08_c02.html) |
| `STILE_R08_C03` | R08 C03 | **(-1.0°, +35.5°)** | (-0.5°, +36.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r08_c03.html`](terrain_grid/stiles_html/stile_r08_c03.html) |
| `STILE_R08_C04` | R08 C04 | **(-1.0°, +36.0°)** | (-0.5°, +36.5°) | 1750–4001 m | Kenya — Central Rift Valley (Nakuru, Naivasha & Aberdare) | Kenya | [`stile_r08_c04.html`](terrain_grid/stiles_html/stile_r08_c04.html) |
| `STILE_R08_C05` | R08 C05 | **(-1.0°, +36.5°)** | (-0.5°, +37.0°) | 1750–4001 m | Kenya — Central Rift Valley (Nakuru, Naivasha & Aberdare) | Kenya | [`stile_r08_c05.html`](terrain_grid/stiles_html/stile_r08_c05.html) |
| `STILE_R08_C06` | R08 C06 | **(-1.0°, +37.0°)** | (-0.5°, +37.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r08_c06.html`](terrain_grid/stiles_html/stile_r08_c06.html) |
| `STILE_R08_C07` | R08 C07 | **(-1.0°, +37.5°)** | (-0.5°, +38.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r08_c07.html`](terrain_grid/stiles_html/stile_r08_c07.html) |
| `STILE_R08_C08` | R08 C08 | **(-1.0°, +38.0°)** | (-0.5°, +38.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r08_c08.html`](terrain_grid/stiles_html/stile_r08_c08.html) |
| `STILE_R08_C09` | R08 C09 | **(-1.0°, +38.5°)** | (-0.5°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r08_c09.html`](terrain_grid/stiles_html/stile_r08_c09.html) |
| `STILE_R08_C10` | R08 C10 | **(-1.0°, +39.0°)** | (-0.5°, +39.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r08_c10.html`](terrain_grid/stiles_html/stile_r08_c10.html) |
| `STILE_R08_C11` | R08 C11 | **(-1.0°, +39.5°)** | (-0.5°, +40.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r08_c11.html`](terrain_grid/stiles_html/stile_r08_c11.html) |
| `STILE_R08_C12` | R08 C12 | **(-1.0°, +40.0°)** | (-0.5°, +40.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r08_c12.html`](terrain_grid/stiles_html/stile_r08_c12.html) |
| `STILE_R08_C13` | R08 C13 | **(-1.0°, +40.5°)** | (-0.5°, +41.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r08_c13.html`](terrain_grid/stiles_html/stile_r08_c13.html) |
| `STILE_R08_C14` | R08 C14 | **(-1.0°, +41.0°)** | (-0.5°, +41.5°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r08_c14.html`](terrain_grid/stiles_html/stile_r08_c14.html) |
| `STILE_R08_C15` | R08 C15 | **(-1.0°, +41.5°)** | (-0.5°, +42.0°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r08_c15.html`](terrain_grid/stiles_html/stile_r08_c15.html) |
| `STILE_R09_C00` | R09 C00 | **(-0.5°, +34.0°)** | (+0.0°, +34.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r09_c00.html`](terrain_grid/stiles_html/stile_r09_c00.html) |
| `STILE_R09_C01` | R09 C01 | **(-0.5°, +34.5°)** | (+0.0°, +35.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r09_c01.html`](terrain_grid/stiles_html/stile_r09_c01.html) |
| `STILE_R09_C02` | R09 C02 | **(-0.5°, +35.0°)** | (+0.0°, +35.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r09_c02.html`](terrain_grid/stiles_html/stile_r09_c02.html) |
| `STILE_R09_C03` | R09 C03 | **(-0.5°, +35.5°)** | (+0.0°, +36.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r09_c03.html`](terrain_grid/stiles_html/stile_r09_c03.html) |
| `STILE_R09_C04` | R09 C04 | **(-0.5°, +36.0°)** | (+0.0°, +36.5°) | 1750–4001 m | Kenya — Central Rift Valley (Nakuru, Naivasha & Aberdare) | Kenya | [`stile_r09_c04.html`](terrain_grid/stiles_html/stile_r09_c04.html) |
| `STILE_R09_C05` | R09 C05 | **(-0.5°, +36.5°)** | (+0.0°, +37.0°) | 1750–4001 m | Kenya — Central Rift Valley (Nakuru, Naivasha & Aberdare) | Kenya | [`stile_r09_c05.html`](terrain_grid/stiles_html/stile_r09_c05.html) |
| `STILE_R09_C06` | R09 C06 | **(-0.5°, +37.0°)** | (+0.0°, +37.5°) | 1200–5199 m | Kenya — Mount Kenya Volcanic Massif & Central Highlands | Kenya | [`stile_r09_c06.html`](terrain_grid/stiles_html/stile_r09_c06.html) |
| `STILE_R09_C07` | R09 C07 | **(-0.5°, +37.5°)** | (+0.0°, +38.0°) | 1200–5199 m | Kenya — Mount Kenya Volcanic Massif & Central Highlands | Kenya | [`stile_r09_c07.html`](terrain_grid/stiles_html/stile_r09_c07.html) |
| `STILE_R09_C08` | R09 C08 | **(-0.5°, +38.0°)** | (+0.0°, +38.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r09_c08.html`](terrain_grid/stiles_html/stile_r09_c08.html) |
| `STILE_R09_C09` | R09 C09 | **(-0.5°, +38.5°)** | (+0.0°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r09_c09.html`](terrain_grid/stiles_html/stile_r09_c09.html) |
| `STILE_R09_C10` | R09 C10 | **(-0.5°, +39.0°)** | (+0.0°, +39.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r09_c10.html`](terrain_grid/stiles_html/stile_r09_c10.html) |
| `STILE_R09_C11` | R09 C11 | **(-0.5°, +39.5°)** | (+0.0°, +40.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r09_c11.html`](terrain_grid/stiles_html/stile_r09_c11.html) |
| `STILE_R09_C12` | R09 C12 | **(-0.5°, +40.0°)** | (+0.0°, +40.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r09_c12.html`](terrain_grid/stiles_html/stile_r09_c12.html) |
| `STILE_R09_C13` | R09 C13 | **(-0.5°, +40.5°)** | (+0.0°, +41.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r09_c13.html`](terrain_grid/stiles_html/stile_r09_c13.html) |
| `STILE_R09_C14` | R09 C14 | **(-0.5°, +41.0°)** | (+0.0°, +41.5°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r09_c14.html`](terrain_grid/stiles_html/stile_r09_c14.html) |
| `STILE_R09_C15` | R09 C15 | **(-0.5°, +41.5°)** | (+0.0°, +42.0°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r09_c15.html`](terrain_grid/stiles_html/stile_r09_c15.html) |
| `STILE_R10_C00` | R10 C00 | **(+0.0°, +34.0°)** | (+0.5°, +34.5°) | 1135–1750 m | Kenya — Lake Victoria Basin & Winam Gulf | Kenya | [`stile_r10_c00.html`](terrain_grid/stiles_html/stile_r10_c00.html) |
| `STILE_R10_C01` | R10 C01 | **(+0.0°, +34.5°)** | (+0.5°, +35.0°) | 1135–1750 m | Kenya — Lake Victoria Basin & Winam Gulf | Kenya | [`stile_r10_c01.html`](terrain_grid/stiles_html/stile_r10_c01.html) |
| `STILE_R10_C02` | R10 C02 | **(+0.0°, +35.0°)** | (+0.5°, +35.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r10_c02.html`](terrain_grid/stiles_html/stile_r10_c02.html) |
| `STILE_R10_C03` | R10 C03 | **(+0.0°, +35.5°)** | (+0.5°, +36.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r10_c03.html`](terrain_grid/stiles_html/stile_r10_c03.html) |
| `STILE_R10_C04` | R10 C04 | **(+0.0°, +36.0°)** | (+0.5°, +36.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r10_c04.html`](terrain_grid/stiles_html/stile_r10_c04.html) |
| `STILE_R10_C05` | R10 C05 | **(+0.0°, +36.5°)** | (+0.5°, +37.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r10_c05.html`](terrain_grid/stiles_html/stile_r10_c05.html) |
| `STILE_R10_C06` | R10 C06 | **(+0.0°, +37.0°)** | (+0.5°, +37.5°) | 1200–5199 m | Kenya — Mount Kenya Volcanic Massif & Central Highlands | Kenya | [`stile_r10_c06.html`](terrain_grid/stiles_html/stile_r10_c06.html) |
| `STILE_R10_C07` | R10 C07 | **(+0.0°, +37.5°)** | (+0.5°, +38.0°) | 1200–5199 m | Kenya — Mount Kenya Volcanic Massif & Central Highlands | Kenya | [`stile_r10_c07.html`](terrain_grid/stiles_html/stile_r10_c07.html) |
| `STILE_R10_C08` | R10 C08 | **(+0.0°, +38.0°)** | (+0.5°, +38.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r10_c08.html`](terrain_grid/stiles_html/stile_r10_c08.html) |
| `STILE_R10_C09` | R10 C09 | **(+0.0°, +38.5°)** | (+0.5°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r10_c09.html`](terrain_grid/stiles_html/stile_r10_c09.html) |
| `STILE_R10_C10` | R10 C10 | **(+0.0°, +39.0°)** | (+0.5°, +39.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r10_c10.html`](terrain_grid/stiles_html/stile_r10_c10.html) |
| `STILE_R10_C11` | R10 C11 | **(+0.0°, +39.5°)** | (+0.5°, +40.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r10_c11.html`](terrain_grid/stiles_html/stile_r10_c11.html) |
| `STILE_R10_C12` | R10 C12 | **(+0.0°, +40.0°)** | (+0.5°, +40.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r10_c12.html`](terrain_grid/stiles_html/stile_r10_c12.html) |
| `STILE_R10_C13` | R10 C13 | **(+0.0°, +40.5°)** | (+0.5°, +41.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r10_c13.html`](terrain_grid/stiles_html/stile_r10_c13.html) |
| `STILE_R10_C14` | R10 C14 | **(+0.0°, +41.0°)** | (+0.5°, +41.5°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r10_c14.html`](terrain_grid/stiles_html/stile_r10_c14.html) |
| `STILE_R10_C15` | R10 C15 | **(+0.0°, +41.5°)** | (+0.5°, +42.0°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r10_c15.html`](terrain_grid/stiles_html/stile_r10_c15.html) |
| `STILE_R11_C00` | R11 C00 | **(+0.5°, +34.0°)** | (+1.0°, +34.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r11_c00.html`](terrain_grid/stiles_html/stile_r11_c00.html) |
| `STILE_R11_C01` | R11 C01 | **(+0.5°, +34.5°)** | (+1.0°, +35.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r11_c01.html`](terrain_grid/stiles_html/stile_r11_c01.html) |
| `STILE_R11_C02` | R11 C02 | **(+0.5°, +35.0°)** | (+1.0°, +35.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r11_c02.html`](terrain_grid/stiles_html/stile_r11_c02.html) |
| `STILE_R11_C03` | R11 C03 | **(+0.5°, +35.5°)** | (+1.0°, +36.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r11_c03.html`](terrain_grid/stiles_html/stile_r11_c03.html) |
| `STILE_R11_C04` | R11 C04 | **(+0.5°, +36.0°)** | (+1.0°, +36.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r11_c04.html`](terrain_grid/stiles_html/stile_r11_c04.html) |
| `STILE_R11_C05` | R11 C05 | **(+0.5°, +36.5°)** | (+1.0°, +37.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r11_c05.html`](terrain_grid/stiles_html/stile_r11_c05.html) |
| `STILE_R11_C06` | R11 C06 | **(+0.5°, +37.0°)** | (+1.0°, +37.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r11_c06.html`](terrain_grid/stiles_html/stile_r11_c06.html) |
| `STILE_R11_C07` | R11 C07 | **(+0.5°, +37.5°)** | (+1.0°, +38.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r11_c07.html`](terrain_grid/stiles_html/stile_r11_c07.html) |
| `STILE_R11_C08` | R11 C08 | **(+0.5°, +38.0°)** | (+1.0°, +38.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r11_c08.html`](terrain_grid/stiles_html/stile_r11_c08.html) |
| `STILE_R11_C09` | R11 C09 | **(+0.5°, +38.5°)** | (+1.0°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r11_c09.html`](terrain_grid/stiles_html/stile_r11_c09.html) |
| `STILE_R11_C10` | R11 C10 | **(+0.5°, +39.0°)** | (+1.0°, +39.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r11_c10.html`](terrain_grid/stiles_html/stile_r11_c10.html) |
| `STILE_R11_C11` | R11 C11 | **(+0.5°, +39.5°)** | (+1.0°, +40.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r11_c11.html`](terrain_grid/stiles_html/stile_r11_c11.html) |
| `STILE_R11_C12` | R11 C12 | **(+0.5°, +40.0°)** | (+1.0°, +40.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r11_c12.html`](terrain_grid/stiles_html/stile_r11_c12.html) |
| `STILE_R11_C13` | R11 C13 | **(+0.5°, +40.5°)** | (+1.0°, +41.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r11_c13.html`](terrain_grid/stiles_html/stile_r11_c13.html) |
| `STILE_R11_C14` | R11 C14 | **(+0.5°, +41.0°)** | (+1.0°, +41.5°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r11_c14.html`](terrain_grid/stiles_html/stile_r11_c14.html) |
| `STILE_R11_C15` | R11 C15 | **(+0.5°, +41.5°)** | (+1.0°, +42.0°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r11_c15.html`](terrain_grid/stiles_html/stile_r11_c15.html) |
| `STILE_R12_C00` | R12 C00 | **(+1.0°, +34.0°)** | (+1.5°, +34.5°) | 1135–1900 m | Border Zone — Eastern Uganda & Lake Victoria Shore | Buffer | [`stile_r12_c00.html`](terrain_grid/stiles_html/stile_r12_c00.html) |
| `STILE_R12_C01` | R12 C01 | **(+1.0°, +34.5°)** | (+1.5°, +35.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r12_c01.html`](terrain_grid/stiles_html/stile_r12_c01.html) |
| `STILE_R12_C02` | R12 C02 | **(+1.0°, +35.0°)** | (+1.5°, +35.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r12_c02.html`](terrain_grid/stiles_html/stile_r12_c02.html) |
| `STILE_R12_C03` | R12 C03 | **(+1.0°, +35.5°)** | (+1.5°, +36.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r12_c03.html`](terrain_grid/stiles_html/stile_r12_c03.html) |
| `STILE_R12_C04` | R12 C04 | **(+1.0°, +36.0°)** | (+1.5°, +36.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r12_c04.html`](terrain_grid/stiles_html/stile_r12_c04.html) |
| `STILE_R12_C05` | R12 C05 | **(+1.0°, +36.5°)** | (+1.5°, +37.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r12_c05.html`](terrain_grid/stiles_html/stile_r12_c05.html) |
| `STILE_R12_C06` | R12 C06 | **(+1.0°, +37.0°)** | (+1.5°, +37.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r12_c06.html`](terrain_grid/stiles_html/stile_r12_c06.html) |
| `STILE_R12_C07` | R12 C07 | **(+1.0°, +37.5°)** | (+1.5°, +38.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r12_c07.html`](terrain_grid/stiles_html/stile_r12_c07.html) |
| `STILE_R12_C08` | R12 C08 | **(+1.0°, +38.0°)** | (+1.5°, +38.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r12_c08.html`](terrain_grid/stiles_html/stile_r12_c08.html) |
| `STILE_R12_C09` | R12 C09 | **(+1.0°, +38.5°)** | (+1.5°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r12_c09.html`](terrain_grid/stiles_html/stile_r12_c09.html) |
| `STILE_R12_C10` | R12 C10 | **(+1.0°, +39.0°)** | (+1.5°, +39.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r12_c10.html`](terrain_grid/stiles_html/stile_r12_c10.html) |
| `STILE_R12_C11` | R12 C11 | **(+1.0°, +39.5°)** | (+1.5°, +40.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r12_c11.html`](terrain_grid/stiles_html/stile_r12_c11.html) |
| `STILE_R12_C12` | R12 C12 | **(+1.0°, +40.0°)** | (+1.5°, +40.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r12_c12.html`](terrain_grid/stiles_html/stile_r12_c12.html) |
| `STILE_R12_C13` | R12 C13 | **(+1.0°, +40.5°)** | (+1.5°, +41.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r12_c13.html`](terrain_grid/stiles_html/stile_r12_c13.html) |
| `STILE_R12_C14` | R12 C14 | **(+1.0°, +41.0°)** | (+1.5°, +41.5°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r12_c14.html`](terrain_grid/stiles_html/stile_r12_c14.html) |
| `STILE_R12_C15` | R12 C15 | **(+1.0°, +41.5°)** | (+1.5°, +42.0°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r12_c15.html`](terrain_grid/stiles_html/stile_r12_c15.html) |
| `STILE_R13_C00` | R13 C00 | **(+1.5°, +34.0°)** | (+2.0°, +34.5°) | 1135–1900 m | Border Zone — Eastern Uganda & Lake Victoria Shore | Buffer | [`stile_r13_c00.html`](terrain_grid/stiles_html/stile_r13_c00.html) |
| `STILE_R13_C01` | R13 C01 | **(+1.5°, +34.5°)** | (+2.0°, +35.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r13_c01.html`](terrain_grid/stiles_html/stile_r13_c01.html) |
| `STILE_R13_C02` | R13 C02 | **(+1.5°, +35.0°)** | (+2.0°, +35.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r13_c02.html`](terrain_grid/stiles_html/stile_r13_c02.html) |
| `STILE_R13_C03` | R13 C03 | **(+1.5°, +35.5°)** | (+2.0°, +36.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r13_c03.html`](terrain_grid/stiles_html/stile_r13_c03.html) |
| `STILE_R13_C04` | R13 C04 | **(+1.5°, +36.0°)** | (+2.0°, +36.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r13_c04.html`](terrain_grid/stiles_html/stile_r13_c04.html) |
| `STILE_R13_C05` | R13 C05 | **(+1.5°, +36.5°)** | (+2.0°, +37.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r13_c05.html`](terrain_grid/stiles_html/stile_r13_c05.html) |
| `STILE_R13_C06` | R13 C06 | **(+1.5°, +37.0°)** | (+2.0°, +37.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r13_c06.html`](terrain_grid/stiles_html/stile_r13_c06.html) |
| `STILE_R13_C07` | R13 C07 | **(+1.5°, +37.5°)** | (+2.0°, +38.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r13_c07.html`](terrain_grid/stiles_html/stile_r13_c07.html) |
| `STILE_R13_C08` | R13 C08 | **(+1.5°, +38.0°)** | (+2.0°, +38.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r13_c08.html`](terrain_grid/stiles_html/stile_r13_c08.html) |
| `STILE_R13_C09` | R13 C09 | **(+1.5°, +38.5°)** | (+2.0°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r13_c09.html`](terrain_grid/stiles_html/stile_r13_c09.html) |
| `STILE_R13_C10` | R13 C10 | **(+1.5°, +39.0°)** | (+2.0°, +39.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r13_c10.html`](terrain_grid/stiles_html/stile_r13_c10.html) |
| `STILE_R13_C11` | R13 C11 | **(+1.5°, +39.5°)** | (+2.0°, +40.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r13_c11.html`](terrain_grid/stiles_html/stile_r13_c11.html) |
| `STILE_R13_C12` | R13 C12 | **(+1.5°, +40.0°)** | (+2.0°, +40.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r13_c12.html`](terrain_grid/stiles_html/stile_r13_c12.html) |
| `STILE_R13_C13` | R13 C13 | **(+1.5°, +40.5°)** | (+2.0°, +41.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r13_c13.html`](terrain_grid/stiles_html/stile_r13_c13.html) |
| `STILE_R13_C14` | R13 C14 | **(+1.5°, +41.0°)** | (+2.0°, +41.5°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r13_c14.html`](terrain_grid/stiles_html/stile_r13_c14.html) |
| `STILE_R13_C15` | R13 C15 | **(+1.5°, +41.5°)** | (+2.0°, +42.0°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r13_c15.html`](terrain_grid/stiles_html/stile_r13_c15.html) |
| `STILE_R14_C00` | R14 C00 | **(+2.0°, +34.0°)** | (+2.5°, +34.5°) | 1135–1900 m | Border Zone — Eastern Uganda & Lake Victoria Shore | Buffer | [`stile_r14_c00.html`](terrain_grid/stiles_html/stile_r14_c00.html) |
| `STILE_R14_C01` | R14 C01 | **(+2.0°, +34.5°)** | (+2.5°, +35.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r14_c01.html`](terrain_grid/stiles_html/stile_r14_c01.html) |
| `STILE_R14_C02` | R14 C02 | **(+2.0°, +35.0°)** | (+2.5°, +35.5°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r14_c02.html`](terrain_grid/stiles_html/stile_r14_c02.html) |
| `STILE_R14_C03` | R14 C03 | **(+2.0°, +35.5°)** | (+2.5°, +36.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r14_c03.html`](terrain_grid/stiles_html/stile_r14_c03.html) |
| `STILE_R14_C04` | R14 C04 | **(+2.0°, +36.0°)** | (+2.5°, +36.5°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r14_c04.html`](terrain_grid/stiles_html/stile_r14_c04.html) |
| `STILE_R14_C05` | R14 C05 | **(+2.0°, +36.5°)** | (+2.5°, +37.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r14_c05.html`](terrain_grid/stiles_html/stile_r14_c05.html) |
| `STILE_R14_C06` | R14 C06 | **(+2.0°, +37.0°)** | (+2.5°, +37.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r14_c06.html`](terrain_grid/stiles_html/stile_r14_c06.html) |
| `STILE_R14_C07` | R14 C07 | **(+2.0°, +37.5°)** | (+2.5°, +38.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r14_c07.html`](terrain_grid/stiles_html/stile_r14_c07.html) |
| `STILE_R14_C08` | R14 C08 | **(+2.0°, +38.0°)** | (+2.5°, +38.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r14_c08.html`](terrain_grid/stiles_html/stile_r14_c08.html) |
| `STILE_R14_C09` | R14 C09 | **(+2.0°, +38.5°)** | (+2.5°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r14_c09.html`](terrain_grid/stiles_html/stile_r14_c09.html) |
| `STILE_R14_C10` | R14 C10 | **(+2.0°, +39.0°)** | (+2.5°, +39.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r14_c10.html`](terrain_grid/stiles_html/stile_r14_c10.html) |
| `STILE_R14_C11` | R14 C11 | **(+2.0°, +39.5°)** | (+2.5°, +40.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r14_c11.html`](terrain_grid/stiles_html/stile_r14_c11.html) |
| `STILE_R14_C12` | R14 C12 | **(+2.0°, +40.0°)** | (+2.5°, +40.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r14_c12.html`](terrain_grid/stiles_html/stile_r14_c12.html) |
| `STILE_R14_C13` | R14 C13 | **(+2.0°, +40.5°)** | (+2.5°, +41.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r14_c13.html`](terrain_grid/stiles_html/stile_r14_c13.html) |
| `STILE_R14_C14` | R14 C14 | **(+2.0°, +41.0°)** | (+2.5°, +41.5°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r14_c14.html`](terrain_grid/stiles_html/stile_r14_c14.html) |
| `STILE_R14_C15` | R14 C15 | **(+2.0°, +41.5°)** | (+2.5°, +42.0°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r14_c15.html`](terrain_grid/stiles_html/stile_r14_c15.html) |
| `STILE_R15_C00` | R15 C00 | **(+2.5°, +34.0°)** | (+3.0°, +34.5°) | 1135–1900 m | Border Zone — Eastern Uganda & Lake Victoria Shore | Buffer | [`stile_r15_c00.html`](terrain_grid/stiles_html/stile_r15_c00.html) |
| `STILE_R15_C01` | R15 C01 | **(+2.5°, +34.5°)** | (+3.0°, +35.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r15_c01.html`](terrain_grid/stiles_html/stile_r15_c01.html) |
| `STILE_R15_C02` | R15 C02 | **(+2.5°, +35.0°)** | (+3.0°, +35.5°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r15_c02.html`](terrain_grid/stiles_html/stile_r15_c02.html) |
| `STILE_R15_C03` | R15 C03 | **(+2.5°, +35.5°)** | (+3.0°, +36.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r15_c03.html`](terrain_grid/stiles_html/stile_r15_c03.html) |
| `STILE_R15_C04` | R15 C04 | **(+2.5°, +36.0°)** | (+3.0°, +36.5°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r15_c04.html`](terrain_grid/stiles_html/stile_r15_c04.html) |
| `STILE_R15_C05` | R15 C05 | **(+2.5°, +36.5°)** | (+3.0°, +37.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r15_c05.html`](terrain_grid/stiles_html/stile_r15_c05.html) |
| `STILE_R15_C06` | R15 C06 | **(+2.5°, +37.0°)** | (+3.0°, +37.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r15_c06.html`](terrain_grid/stiles_html/stile_r15_c06.html) |
| `STILE_R15_C07` | R15 C07 | **(+2.5°, +37.5°)** | (+3.0°, +38.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r15_c07.html`](terrain_grid/stiles_html/stile_r15_c07.html) |
| `STILE_R15_C08` | R15 C08 | **(+2.5°, +38.0°)** | (+3.0°, +38.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r15_c08.html`](terrain_grid/stiles_html/stile_r15_c08.html) |
| `STILE_R15_C09` | R15 C09 | **(+2.5°, +38.5°)** | (+3.0°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r15_c09.html`](terrain_grid/stiles_html/stile_r15_c09.html) |
| `STILE_R15_C10` | R15 C10 | **(+2.5°, +39.0°)** | (+3.0°, +39.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r15_c10.html`](terrain_grid/stiles_html/stile_r15_c10.html) |
| `STILE_R15_C11` | R15 C11 | **(+2.5°, +39.5°)** | (+3.0°, +40.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r15_c11.html`](terrain_grid/stiles_html/stile_r15_c11.html) |
| `STILE_R15_C12` | R15 C12 | **(+2.5°, +40.0°)** | (+3.0°, +40.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r15_c12.html`](terrain_grid/stiles_html/stile_r15_c12.html) |
| `STILE_R15_C13` | R15 C13 | **(+2.5°, +40.5°)** | (+3.0°, +41.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r15_c13.html`](terrain_grid/stiles_html/stile_r15_c13.html) |
| `STILE_R15_C14` | R15 C14 | **(+2.5°, +41.0°)** | (+3.0°, +41.5°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r15_c14.html`](terrain_grid/stiles_html/stile_r15_c14.html) |
| `STILE_R15_C15` | R15 C15 | **(+2.5°, +41.5°)** | (+3.0°, +42.0°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r15_c15.html`](terrain_grid/stiles_html/stile_r15_c15.html) |
| `STILE_R16_C00` | R16 C00 | **(+3.0°, +34.0°)** | (+3.5°, +34.5°) | 1135–1900 m | Border Zone — Eastern Uganda & Lake Victoria Shore | Buffer | [`stile_r16_c00.html`](terrain_grid/stiles_html/stile_r16_c00.html) |
| `STILE_R16_C01` | R16 C01 | **(+3.0°, +34.5°)** | (+3.5°, +35.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r16_c01.html`](terrain_grid/stiles_html/stile_r16_c01.html) |
| `STILE_R16_C02` | R16 C02 | **(+3.0°, +35.0°)** | (+3.5°, +35.5°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r16_c02.html`](terrain_grid/stiles_html/stile_r16_c02.html) |
| `STILE_R16_C03` | R16 C03 | **(+3.0°, +35.5°)** | (+3.5°, +36.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r16_c03.html`](terrain_grid/stiles_html/stile_r16_c03.html) |
| `STILE_R16_C04` | R16 C04 | **(+3.0°, +36.0°)** | (+3.5°, +36.5°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r16_c04.html`](terrain_grid/stiles_html/stile_r16_c04.html) |
| `STILE_R16_C05` | R16 C05 | **(+3.0°, +36.5°)** | (+3.5°, +37.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r16_c05.html`](terrain_grid/stiles_html/stile_r16_c05.html) |
| `STILE_R16_C06` | R16 C06 | **(+3.0°, +37.0°)** | (+3.5°, +37.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r16_c06.html`](terrain_grid/stiles_html/stile_r16_c06.html) |
| `STILE_R16_C07` | R16 C07 | **(+3.0°, +37.5°)** | (+3.5°, +38.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r16_c07.html`](terrain_grid/stiles_html/stile_r16_c07.html) |
| `STILE_R16_C08` | R16 C08 | **(+3.0°, +38.0°)** | (+3.5°, +38.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r16_c08.html`](terrain_grid/stiles_html/stile_r16_c08.html) |
| `STILE_R16_C09` | R16 C09 | **(+3.0°, +38.5°)** | (+3.5°, +39.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r16_c09.html`](terrain_grid/stiles_html/stile_r16_c09.html) |
| `STILE_R16_C10` | R16 C10 | **(+3.0°, +39.0°)** | (+3.5°, +39.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r16_c10.html`](terrain_grid/stiles_html/stile_r16_c10.html) |
| `STILE_R16_C11` | R16 C11 | **(+3.0°, +39.5°)** | (+3.5°, +40.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r16_c11.html`](terrain_grid/stiles_html/stile_r16_c11.html) |
| `STILE_R16_C12` | R16 C12 | **(+3.0°, +40.0°)** | (+3.5°, +40.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r16_c12.html`](terrain_grid/stiles_html/stile_r16_c12.html) |
| `STILE_R16_C13` | R16 C13 | **(+3.0°, +40.5°)** | (+3.5°, +41.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r16_c13.html`](terrain_grid/stiles_html/stile_r16_c13.html) |
| `STILE_R16_C14` | R16 C14 | **(+3.0°, +41.0°)** | (+3.5°, +41.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r16_c14.html`](terrain_grid/stiles_html/stile_r16_c14.html) |
| `STILE_R16_C15` | R16 C15 | **(+3.0°, +41.5°)** | (+3.5°, +42.0°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r16_c15.html`](terrain_grid/stiles_html/stile_r16_c15.html) |
| `STILE_R17_C00` | R17 C00 | **(+3.5°, +34.0°)** | (+4.0°, +34.5°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r17_c00.html`](terrain_grid/stiles_html/stile_r17_c00.html) |
| `STILE_R17_C01` | R17 C01 | **(+3.5°, +34.5°)** | (+4.0°, +35.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r17_c01.html`](terrain_grid/stiles_html/stile_r17_c01.html) |
| `STILE_R17_C02` | R17 C02 | **(+3.5°, +35.0°)** | (+4.0°, +35.5°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r17_c02.html`](terrain_grid/stiles_html/stile_r17_c02.html) |
| `STILE_R17_C03` | R17 C03 | **(+3.5°, +35.5°)** | (+4.0°, +36.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r17_c03.html`](terrain_grid/stiles_html/stile_r17_c03.html) |
| `STILE_R17_C04` | R17 C04 | **(+3.5°, +36.0°)** | (+4.0°, +36.5°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r17_c04.html`](terrain_grid/stiles_html/stile_r17_c04.html) |
| `STILE_R17_C05` | R17 C05 | **(+3.5°, +36.5°)** | (+4.0°, +37.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r17_c05.html`](terrain_grid/stiles_html/stile_r17_c05.html) |
| `STILE_R17_C06` | R17 C06 | **(+3.5°, +37.0°)** | (+4.0°, +37.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r17_c06.html`](terrain_grid/stiles_html/stile_r17_c06.html) |
| `STILE_R17_C07` | R17 C07 | **(+3.5°, +37.5°)** | (+4.0°, +38.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r17_c07.html`](terrain_grid/stiles_html/stile_r17_c07.html) |
| `STILE_R17_C08` | R17 C08 | **(+3.5°, +38.0°)** | (+4.0°, +38.5°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r17_c08.html`](terrain_grid/stiles_html/stile_r17_c08.html) |
| `STILE_R17_C09` | R17 C09 | **(+3.5°, +38.5°)** | (+4.0°, +39.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r17_c09.html`](terrain_grid/stiles_html/stile_r17_c09.html) |
| `STILE_R17_C10` | R17 C10 | **(+3.5°, +39.0°)** | (+4.0°, +39.5°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r17_c10.html`](terrain_grid/stiles_html/stile_r17_c10.html) |
| `STILE_R17_C11` | R17 C11 | **(+3.5°, +39.5°)** | (+4.0°, +40.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r17_c11.html`](terrain_grid/stiles_html/stile_r17_c11.html) |
| `STILE_R17_C12` | R17 C12 | **(+3.5°, +40.0°)** | (+4.0°, +40.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r17_c12.html`](terrain_grid/stiles_html/stile_r17_c12.html) |
| `STILE_R17_C13` | R17 C13 | **(+3.5°, +40.5°)** | (+4.0°, +41.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r17_c13.html`](terrain_grid/stiles_html/stile_r17_c13.html) |
| `STILE_R17_C14` | R17 C14 | **(+3.5°, +41.0°)** | (+4.0°, +41.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r17_c14.html`](terrain_grid/stiles_html/stile_r17_c14.html) |
| `STILE_R17_C15` | R17 C15 | **(+3.5°, +41.5°)** | (+4.0°, +42.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r17_c15.html`](terrain_grid/stiles_html/stile_r17_c15.html) |
| `STILE_R18_C00` | R18 C00 | **(+4.0°, +34.0°)** | (+4.5°, +34.5°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r18_c00.html`](terrain_grid/stiles_html/stile_r18_c00.html) |
| `STILE_R18_C01` | R18 C01 | **(+4.0°, +34.5°)** | (+4.5°, +35.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r18_c01.html`](terrain_grid/stiles_html/stile_r18_c01.html) |
| `STILE_R18_C02` | R18 C02 | **(+4.0°, +35.0°)** | (+4.5°, +35.5°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r18_c02.html`](terrain_grid/stiles_html/stile_r18_c02.html) |
| `STILE_R18_C03` | R18 C03 | **(+4.0°, +35.5°)** | (+4.5°, +36.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r18_c03.html`](terrain_grid/stiles_html/stile_r18_c03.html) |
| `STILE_R18_C04` | R18 C04 | **(+4.0°, +36.0°)** | (+4.5°, +36.5°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r18_c04.html`](terrain_grid/stiles_html/stile_r18_c04.html) |
| `STILE_R18_C05` | R18 C05 | **(+4.0°, +36.5°)** | (+4.5°, +37.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r18_c05.html`](terrain_grid/stiles_html/stile_r18_c05.html) |
| `STILE_R18_C06` | R18 C06 | **(+4.0°, +37.0°)** | (+4.5°, +37.5°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r18_c06.html`](terrain_grid/stiles_html/stile_r18_c06.html) |
| `STILE_R18_C07` | R18 C07 | **(+4.0°, +37.5°)** | (+4.5°, +38.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r18_c07.html`](terrain_grid/stiles_html/stile_r18_c07.html) |
| `STILE_R18_C08` | R18 C08 | **(+4.0°, +38.0°)** | (+4.5°, +38.5°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r18_c08.html`](terrain_grid/stiles_html/stile_r18_c08.html) |
| `STILE_R18_C09` | R18 C09 | **(+4.0°, +38.5°)** | (+4.5°, +39.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r18_c09.html`](terrain_grid/stiles_html/stile_r18_c09.html) |
| `STILE_R18_C10` | R18 C10 | **(+4.0°, +39.0°)** | (+4.5°, +39.5°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r18_c10.html`](terrain_grid/stiles_html/stile_r18_c10.html) |
| `STILE_R18_C11` | R18 C11 | **(+4.0°, +39.5°)** | (+4.5°, +40.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r18_c11.html`](terrain_grid/stiles_html/stile_r18_c11.html) |
| `STILE_R18_C12` | R18 C12 | **(+4.0°, +40.0°)** | (+4.5°, +40.5°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r18_c12.html`](terrain_grid/stiles_html/stile_r18_c12.html) |
| `STILE_R18_C13` | R18 C13 | **(+4.0°, +40.5°)** | (+4.5°, +41.0°) | 350–1450 m | Kenya — Inland Nyika Plateau & Savanna | Kenya | [`stile_r18_c13.html`](terrain_grid/stiles_html/stile_r18_c13.html) |
| `STILE_R18_C14` | R18 C14 | **(+4.0°, +41.0°)** | (+4.5°, +41.5°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r18_c14.html`](terrain_grid/stiles_html/stile_r18_c14.html) |
| `STILE_R18_C15` | R18 C15 | **(+4.0°, +41.5°)** | (+4.5°, +42.0°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r18_c15.html`](terrain_grid/stiles_html/stile_r18_c15.html) |
| `STILE_R19_C00` | R19 C00 | **(+4.5°, +34.0°)** | (+5.0°, +34.5°) | 1135–1900 m | Border Zone — Eastern Uganda & Lake Victoria Shore | Buffer | [`stile_r19_c00.html`](terrain_grid/stiles_html/stile_r19_c00.html) |
| `STILE_R19_C01` | R19 C01 | **(+4.5°, +34.5°)** | (+5.0°, +35.0°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r19_c01.html`](terrain_grid/stiles_html/stile_r19_c01.html) |
| `STILE_R19_C02` | R19 C02 | **(+4.5°, +35.0°)** | (+5.0°, +35.5°) | 360–1950 m | Kenya — Northern Turkana & Arid Basins | Kenya | [`stile_r19_c02.html`](terrain_grid/stiles_html/stile_r19_c02.html) |
| `STILE_R19_C03` | R19 C03 | **(+4.5°, +35.5°)** | (+5.0°, +36.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r19_c03.html`](terrain_grid/stiles_html/stile_r19_c03.html) |
| `STILE_R19_C04` | R19 C04 | **(+4.5°, +36.0°)** | (+5.0°, +36.5°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r19_c04.html`](terrain_grid/stiles_html/stile_r19_c04.html) |
| `STILE_R19_C05` | R19 C05 | **(+4.5°, +36.5°)** | (+5.0°, +37.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r19_c05.html`](terrain_grid/stiles_html/stile_r19_c05.html) |
| `STILE_R19_C06` | R19 C06 | **(+4.5°, +37.0°)** | (+5.0°, +37.5°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r19_c06.html`](terrain_grid/stiles_html/stile_r19_c06.html) |
| `STILE_R19_C07` | R19 C07 | **(+4.5°, +37.5°)** | (+5.0°, +38.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r19_c07.html`](terrain_grid/stiles_html/stile_r19_c07.html) |
| `STILE_R19_C08` | R19 C08 | **(+4.5°, +38.0°)** | (+5.0°, +38.5°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r19_c08.html`](terrain_grid/stiles_html/stile_r19_c08.html) |
| `STILE_R19_C09` | R19 C09 | **(+4.5°, +38.5°)** | (+5.0°, +39.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r19_c09.html`](terrain_grid/stiles_html/stile_r19_c09.html) |
| `STILE_R19_C10` | R19 C10 | **(+4.5°, +39.0°)** | (+5.0°, +39.5°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r19_c10.html`](terrain_grid/stiles_html/stile_r19_c10.html) |
| `STILE_R19_C11` | R19 C11 | **(+4.5°, +39.5°)** | (+5.0°, +40.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r19_c11.html`](terrain_grid/stiles_html/stile_r19_c11.html) |
| `STILE_R19_C12` | R19 C12 | **(+4.5°, +40.0°)** | (+5.0°, +40.5°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r19_c12.html`](terrain_grid/stiles_html/stile_r19_c12.html) |
| `STILE_R19_C13` | R19 C13 | **(+4.5°, +40.5°)** | (+5.0°, +41.0°) | 500–1500 m | Border Zone — Regional Buffer | Buffer | [`stile_r19_c13.html`](terrain_grid/stiles_html/stile_r19_c13.html) |
| `STILE_R19_C14` | R19 C14 | **(+4.5°, +41.0°)** | (+5.0°, +41.5°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r19_c14.html`](terrain_grid/stiles_html/stile_r19_c14.html) |
| `STILE_R19_C15` | R19 C15 | **(+4.5°, +41.5°)** | (+5.0°, +42.0°) | 120–380 m | Border Zone — Somalia Frontier & Jubaland | Buffer | [`stile_r19_c15.html`](terrain_grid/stiles_html/stile_r19_c15.html) |
| `STILE_R20_C00` | R20 C00 | **(+5.0°, +34.0°)** | (+5.5°, +34.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c00.html`](terrain_grid/stiles_html/stile_r20_c00.html) |
| `STILE_R20_C01` | R20 C01 | **(+5.0°, +34.5°)** | (+5.5°, +35.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c01.html`](terrain_grid/stiles_html/stile_r20_c01.html) |
| `STILE_R20_C02` | R20 C02 | **(+5.0°, +35.0°)** | (+5.5°, +35.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c02.html`](terrain_grid/stiles_html/stile_r20_c02.html) |
| `STILE_R20_C03` | R20 C03 | **(+5.0°, +35.5°)** | (+5.5°, +36.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c03.html`](terrain_grid/stiles_html/stile_r20_c03.html) |
| `STILE_R20_C04` | R20 C04 | **(+5.0°, +36.0°)** | (+5.5°, +36.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c04.html`](terrain_grid/stiles_html/stile_r20_c04.html) |
| `STILE_R20_C05` | R20 C05 | **(+5.0°, +36.5°)** | (+5.5°, +37.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c05.html`](terrain_grid/stiles_html/stile_r20_c05.html) |
| `STILE_R20_C06` | R20 C06 | **(+5.0°, +37.0°)** | (+5.5°, +37.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c06.html`](terrain_grid/stiles_html/stile_r20_c06.html) |
| `STILE_R20_C07` | R20 C07 | **(+5.0°, +37.5°)** | (+5.5°, +38.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c07.html`](terrain_grid/stiles_html/stile_r20_c07.html) |
| `STILE_R20_C08` | R20 C08 | **(+5.0°, +38.0°)** | (+5.5°, +38.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c08.html`](terrain_grid/stiles_html/stile_r20_c08.html) |
| `STILE_R20_C09` | R20 C09 | **(+5.0°, +38.5°)** | (+5.5°, +39.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c09.html`](terrain_grid/stiles_html/stile_r20_c09.html) |
| `STILE_R20_C10` | R20 C10 | **(+5.0°, +39.0°)** | (+5.5°, +39.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c10.html`](terrain_grid/stiles_html/stile_r20_c10.html) |
| `STILE_R20_C11` | R20 C11 | **(+5.0°, +39.5°)** | (+5.5°, +40.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c11.html`](terrain_grid/stiles_html/stile_r20_c11.html) |
| `STILE_R20_C12` | R20 C12 | **(+5.0°, +40.0°)** | (+5.5°, +40.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c12.html`](terrain_grid/stiles_html/stile_r20_c12.html) |
| `STILE_R20_C13` | R20 C13 | **(+5.0°, +40.5°)** | (+5.5°, +41.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c13.html`](terrain_grid/stiles_html/stile_r20_c13.html) |
| `STILE_R20_C14` | R20 C14 | **(+5.0°, +41.0°)** | (+5.5°, +41.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c14.html`](terrain_grid/stiles_html/stile_r20_c14.html) |
| `STILE_R20_C15` | R20 C15 | **(+5.0°, +41.5°)** | (+5.5°, +42.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r20_c15.html`](terrain_grid/stiles_html/stile_r20_c15.html) |
| `STILE_R21_C00` | R21 C00 | **(+5.5°, +34.0°)** | (+6.0°, +34.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c00.html`](terrain_grid/stiles_html/stile_r21_c00.html) |
| `STILE_R21_C01` | R21 C01 | **(+5.5°, +34.5°)** | (+6.0°, +35.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c01.html`](terrain_grid/stiles_html/stile_r21_c01.html) |
| `STILE_R21_C02` | R21 C02 | **(+5.5°, +35.0°)** | (+6.0°, +35.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c02.html`](terrain_grid/stiles_html/stile_r21_c02.html) |
| `STILE_R21_C03` | R21 C03 | **(+5.5°, +35.5°)** | (+6.0°, +36.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c03.html`](terrain_grid/stiles_html/stile_r21_c03.html) |
| `STILE_R21_C04` | R21 C04 | **(+5.5°, +36.0°)** | (+6.0°, +36.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c04.html`](terrain_grid/stiles_html/stile_r21_c04.html) |
| `STILE_R21_C05` | R21 C05 | **(+5.5°, +36.5°)** | (+6.0°, +37.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c05.html`](terrain_grid/stiles_html/stile_r21_c05.html) |
| `STILE_R21_C06` | R21 C06 | **(+5.5°, +37.0°)** | (+6.0°, +37.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c06.html`](terrain_grid/stiles_html/stile_r21_c06.html) |
| `STILE_R21_C07` | R21 C07 | **(+5.5°, +37.5°)** | (+6.0°, +38.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c07.html`](terrain_grid/stiles_html/stile_r21_c07.html) |
| `STILE_R21_C08` | R21 C08 | **(+5.5°, +38.0°)** | (+6.0°, +38.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c08.html`](terrain_grid/stiles_html/stile_r21_c08.html) |
| `STILE_R21_C09` | R21 C09 | **(+5.5°, +38.5°)** | (+6.0°, +39.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c09.html`](terrain_grid/stiles_html/stile_r21_c09.html) |
| `STILE_R21_C10` | R21 C10 | **(+5.5°, +39.0°)** | (+6.0°, +39.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c10.html`](terrain_grid/stiles_html/stile_r21_c10.html) |
| `STILE_R21_C11` | R21 C11 | **(+5.5°, +39.5°)** | (+6.0°, +40.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c11.html`](terrain_grid/stiles_html/stile_r21_c11.html) |
| `STILE_R21_C12` | R21 C12 | **(+5.5°, +40.0°)** | (+6.0°, +40.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c12.html`](terrain_grid/stiles_html/stile_r21_c12.html) |
| `STILE_R21_C13` | R21 C13 | **(+5.5°, +40.5°)** | (+6.0°, +41.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c13.html`](terrain_grid/stiles_html/stile_r21_c13.html) |
| `STILE_R21_C14` | R21 C14 | **(+5.5°, +41.0°)** | (+6.0°, +41.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c14.html`](terrain_grid/stiles_html/stile_r21_c14.html) |
| `STILE_R21_C15` | R21 C15 | **(+5.5°, +41.5°)** | (+6.0°, +42.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r21_c15.html`](terrain_grid/stiles_html/stile_r21_c15.html) |
| `STILE_R22_C00` | R22 C00 | **(+6.0°, +34.0°)** | (+6.5°, +34.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c00.html`](terrain_grid/stiles_html/stile_r22_c00.html) |
| `STILE_R22_C01` | R22 C01 | **(+6.0°, +34.5°)** | (+6.5°, +35.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c01.html`](terrain_grid/stiles_html/stile_r22_c01.html) |
| `STILE_R22_C02` | R22 C02 | **(+6.0°, +35.0°)** | (+6.5°, +35.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c02.html`](terrain_grid/stiles_html/stile_r22_c02.html) |
| `STILE_R22_C03` | R22 C03 | **(+6.0°, +35.5°)** | (+6.5°, +36.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c03.html`](terrain_grid/stiles_html/stile_r22_c03.html) |
| `STILE_R22_C04` | R22 C04 | **(+6.0°, +36.0°)** | (+6.5°, +36.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c04.html`](terrain_grid/stiles_html/stile_r22_c04.html) |
| `STILE_R22_C05` | R22 C05 | **(+6.0°, +36.5°)** | (+6.5°, +37.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c05.html`](terrain_grid/stiles_html/stile_r22_c05.html) |
| `STILE_R22_C06` | R22 C06 | **(+6.0°, +37.0°)** | (+6.5°, +37.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c06.html`](terrain_grid/stiles_html/stile_r22_c06.html) |
| `STILE_R22_C07` | R22 C07 | **(+6.0°, +37.5°)** | (+6.5°, +38.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c07.html`](terrain_grid/stiles_html/stile_r22_c07.html) |
| `STILE_R22_C08` | R22 C08 | **(+6.0°, +38.0°)** | (+6.5°, +38.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c08.html`](terrain_grid/stiles_html/stile_r22_c08.html) |
| `STILE_R22_C09` | R22 C09 | **(+6.0°, +38.5°)** | (+6.5°, +39.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c09.html`](terrain_grid/stiles_html/stile_r22_c09.html) |
| `STILE_R22_C10` | R22 C10 | **(+6.0°, +39.0°)** | (+6.5°, +39.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c10.html`](terrain_grid/stiles_html/stile_r22_c10.html) |
| `STILE_R22_C11` | R22 C11 | **(+6.0°, +39.5°)** | (+6.5°, +40.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c11.html`](terrain_grid/stiles_html/stile_r22_c11.html) |
| `STILE_R22_C12` | R22 C12 | **(+6.0°, +40.0°)** | (+6.5°, +40.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c12.html`](terrain_grid/stiles_html/stile_r22_c12.html) |
| `STILE_R22_C13` | R22 C13 | **(+6.0°, +40.5°)** | (+6.5°, +41.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c13.html`](terrain_grid/stiles_html/stile_r22_c13.html) |
| `STILE_R22_C14` | R22 C14 | **(+6.0°, +41.0°)** | (+6.5°, +41.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c14.html`](terrain_grid/stiles_html/stile_r22_c14.html) |
| `STILE_R22_C15` | R22 C15 | **(+6.0°, +41.5°)** | (+6.5°, +42.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r22_c15.html`](terrain_grid/stiles_html/stile_r22_c15.html) |
| `STILE_R23_C00` | R23 C00 | **(+6.5°, +34.0°)** | (+7.0°, +34.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c00.html`](terrain_grid/stiles_html/stile_r23_c00.html) |
| `STILE_R23_C01` | R23 C01 | **(+6.5°, +34.5°)** | (+7.0°, +35.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c01.html`](terrain_grid/stiles_html/stile_r23_c01.html) |
| `STILE_R23_C02` | R23 C02 | **(+6.5°, +35.0°)** | (+7.0°, +35.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c02.html`](terrain_grid/stiles_html/stile_r23_c02.html) |
| `STILE_R23_C03` | R23 C03 | **(+6.5°, +35.5°)** | (+7.0°, +36.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c03.html`](terrain_grid/stiles_html/stile_r23_c03.html) |
| `STILE_R23_C04` | R23 C04 | **(+6.5°, +36.0°)** | (+7.0°, +36.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c04.html`](terrain_grid/stiles_html/stile_r23_c04.html) |
| `STILE_R23_C05` | R23 C05 | **(+6.5°, +36.5°)** | (+7.0°, +37.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c05.html`](terrain_grid/stiles_html/stile_r23_c05.html) |
| `STILE_R23_C06` | R23 C06 | **(+6.5°, +37.0°)** | (+7.0°, +37.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c06.html`](terrain_grid/stiles_html/stile_r23_c06.html) |
| `STILE_R23_C07` | R23 C07 | **(+6.5°, +37.5°)** | (+7.0°, +38.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c07.html`](terrain_grid/stiles_html/stile_r23_c07.html) |
| `STILE_R23_C08` | R23 C08 | **(+6.5°, +38.0°)** | (+7.0°, +38.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c08.html`](terrain_grid/stiles_html/stile_r23_c08.html) |
| `STILE_R23_C09` | R23 C09 | **(+6.5°, +38.5°)** | (+7.0°, +39.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c09.html`](terrain_grid/stiles_html/stile_r23_c09.html) |
| `STILE_R23_C10` | R23 C10 | **(+6.5°, +39.0°)** | (+7.0°, +39.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c10.html`](terrain_grid/stiles_html/stile_r23_c10.html) |
| `STILE_R23_C11` | R23 C11 | **(+6.5°, +39.5°)** | (+7.0°, +40.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c11.html`](terrain_grid/stiles_html/stile_r23_c11.html) |
| `STILE_R23_C12` | R23 C12 | **(+6.5°, +40.0°)** | (+7.0°, +40.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c12.html`](terrain_grid/stiles_html/stile_r23_c12.html) |
| `STILE_R23_C13` | R23 C13 | **(+6.5°, +40.5°)** | (+7.0°, +41.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c13.html`](terrain_grid/stiles_html/stile_r23_c13.html) |
| `STILE_R23_C14` | R23 C14 | **(+6.5°, +41.0°)** | (+7.0°, +41.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c14.html`](terrain_grid/stiles_html/stile_r23_c14.html) |
| `STILE_R23_C15` | R23 C15 | **(+6.5°, +41.5°)** | (+7.0°, +42.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r23_c15.html`](terrain_grid/stiles_html/stile_r23_c15.html) |
| `STILE_R24_C00` | R24 C00 | **(+7.0°, +34.0°)** | (+7.5°, +34.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c00.html`](terrain_grid/stiles_html/stile_r24_c00.html) |
| `STILE_R24_C01` | R24 C01 | **(+7.0°, +34.5°)** | (+7.5°, +35.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c01.html`](terrain_grid/stiles_html/stile_r24_c01.html) |
| `STILE_R24_C02` | R24 C02 | **(+7.0°, +35.0°)** | (+7.5°, +35.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c02.html`](terrain_grid/stiles_html/stile_r24_c02.html) |
| `STILE_R24_C03` | R24 C03 | **(+7.0°, +35.5°)** | (+7.5°, +36.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c03.html`](terrain_grid/stiles_html/stile_r24_c03.html) |
| `STILE_R24_C04` | R24 C04 | **(+7.0°, +36.0°)** | (+7.5°, +36.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c04.html`](terrain_grid/stiles_html/stile_r24_c04.html) |
| `STILE_R24_C05` | R24 C05 | **(+7.0°, +36.5°)** | (+7.5°, +37.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c05.html`](terrain_grid/stiles_html/stile_r24_c05.html) |
| `STILE_R24_C06` | R24 C06 | **(+7.0°, +37.0°)** | (+7.5°, +37.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c06.html`](terrain_grid/stiles_html/stile_r24_c06.html) |
| `STILE_R24_C07` | R24 C07 | **(+7.0°, +37.5°)** | (+7.5°, +38.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c07.html`](terrain_grid/stiles_html/stile_r24_c07.html) |
| `STILE_R24_C08` | R24 C08 | **(+7.0°, +38.0°)** | (+7.5°, +38.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c08.html`](terrain_grid/stiles_html/stile_r24_c08.html) |
| `STILE_R24_C09` | R24 C09 | **(+7.0°, +38.5°)** | (+7.5°, +39.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c09.html`](terrain_grid/stiles_html/stile_r24_c09.html) |
| `STILE_R24_C10` | R24 C10 | **(+7.0°, +39.0°)** | (+7.5°, +39.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c10.html`](terrain_grid/stiles_html/stile_r24_c10.html) |
| `STILE_R24_C11` | R24 C11 | **(+7.0°, +39.5°)** | (+7.5°, +40.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c11.html`](terrain_grid/stiles_html/stile_r24_c11.html) |
| `STILE_R24_C12` | R24 C12 | **(+7.0°, +40.0°)** | (+7.5°, +40.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c12.html`](terrain_grid/stiles_html/stile_r24_c12.html) |
| `STILE_R24_C13` | R24 C13 | **(+7.0°, +40.5°)** | (+7.5°, +41.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c13.html`](terrain_grid/stiles_html/stile_r24_c13.html) |
| `STILE_R24_C14` | R24 C14 | **(+7.0°, +41.0°)** | (+7.5°, +41.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c14.html`](terrain_grid/stiles_html/stile_r24_c14.html) |
| `STILE_R24_C15` | R24 C15 | **(+7.0°, +41.5°)** | (+7.5°, +42.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r24_c15.html`](terrain_grid/stiles_html/stile_r24_c15.html) |
| `STILE_R25_C00` | R25 C00 | **(+7.5°, +34.0°)** | (+8.0°, +34.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c00.html`](terrain_grid/stiles_html/stile_r25_c00.html) |
| `STILE_R25_C01` | R25 C01 | **(+7.5°, +34.5°)** | (+8.0°, +35.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c01.html`](terrain_grid/stiles_html/stile_r25_c01.html) |
| `STILE_R25_C02` | R25 C02 | **(+7.5°, +35.0°)** | (+8.0°, +35.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c02.html`](terrain_grid/stiles_html/stile_r25_c02.html) |
| `STILE_R25_C03` | R25 C03 | **(+7.5°, +35.5°)** | (+8.0°, +36.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c03.html`](terrain_grid/stiles_html/stile_r25_c03.html) |
| `STILE_R25_C04` | R25 C04 | **(+7.5°, +36.0°)** | (+8.0°, +36.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c04.html`](terrain_grid/stiles_html/stile_r25_c04.html) |
| `STILE_R25_C05` | R25 C05 | **(+7.5°, +36.5°)** | (+8.0°, +37.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c05.html`](terrain_grid/stiles_html/stile_r25_c05.html) |
| `STILE_R25_C06` | R25 C06 | **(+7.5°, +37.0°)** | (+8.0°, +37.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c06.html`](terrain_grid/stiles_html/stile_r25_c06.html) |
| `STILE_R25_C07` | R25 C07 | **(+7.5°, +37.5°)** | (+8.0°, +38.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c07.html`](terrain_grid/stiles_html/stile_r25_c07.html) |
| `STILE_R25_C08` | R25 C08 | **(+7.5°, +38.0°)** | (+8.0°, +38.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c08.html`](terrain_grid/stiles_html/stile_r25_c08.html) |
| `STILE_R25_C09` | R25 C09 | **(+7.5°, +38.5°)** | (+8.0°, +39.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c09.html`](terrain_grid/stiles_html/stile_r25_c09.html) |
| `STILE_R25_C10` | R25 C10 | **(+7.5°, +39.0°)** | (+8.0°, +39.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c10.html`](terrain_grid/stiles_html/stile_r25_c10.html) |
| `STILE_R25_C11` | R25 C11 | **(+7.5°, +39.5°)** | (+8.0°, +40.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c11.html`](terrain_grid/stiles_html/stile_r25_c11.html) |
| `STILE_R25_C12` | R25 C12 | **(+7.5°, +40.0°)** | (+8.0°, +40.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c12.html`](terrain_grid/stiles_html/stile_r25_c12.html) |
| `STILE_R25_C13` | R25 C13 | **(+7.5°, +40.5°)** | (+8.0°, +41.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c13.html`](terrain_grid/stiles_html/stile_r25_c13.html) |
| `STILE_R25_C14` | R25 C14 | **(+7.5°, +41.0°)** | (+8.0°, +41.5°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c14.html`](terrain_grid/stiles_html/stile_r25_c14.html) |
| `STILE_R25_C15` | R25 C15 | **(+7.5°, +41.5°)** | (+8.0°, +42.0°) | 900–2600 m | Northern Extension — Southern Ethiopia Highlands & Rift | Buffer | [`stile_r25_c15.html`](terrain_grid/stiles_html/stile_r25_c15.html) |

---

## 5. File Layout & Deliverables

* **`terrain_grid/kenya_terrain_stiles.sqlite` & `stiles.db`**: SQLite database populated with all 416 records.
* **`terrain_grid/kenya_grid_map.html`**: Master interactive SVG map drawing the 16×26 pure grid with lines at 0.5°.
* **`terrain_grid/stiles_html/stile_rXX_cXX.html`**: 416 individual HTML definition files.
* **Android In-App Screen**: Integrated into Jetpack Compose (`KenyaTerrainGridScreen.kt`).
