# Road Bounding Area Obtaining Logic (Extremes & Square/Rectangular Enclosure)

## 1. Mathematical Concept & Geometric Derivation
1. Straight road from (0,0) to (10,10):
   - Road Length = sqrt(10^2 + 10^2) = sqrt(200) ≈ 14.142
   - Left extreme (Xmin)  = min(0, 10)  = 0
   - Right extreme (Xmax) = max(0, 10)  = 10
   - Down extreme (Ymin)  = min(0, 10)  = 0
   - Up extreme (Ymax)    = max(0, 10)  = 10
   - Corners: (0,0), (10,0), (10,10), (0,10)

## 2. Real-World Kenya Magadi B19/C58 Corridor
- Lon bounds: 36.2500° E to 36.7000° E (Δ ≈ 50 km)
- Lat bounds: -1.9000° S to -1.4500° S (Δ ≈ 50 km)
- Square Enclosure: 50 km x 50 km (2,500 km²)
- 25 Chunks: 5x5 grid of 10 km x 10 km tiles (chunk_00_00 to chunk_04_04)
- Local Metric Units: [0 .. 10,000m] with 1:1:1 scale (zero GPU float32 jitter)
