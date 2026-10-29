/**
 * Almost Sqrt & Bounding Square Derivation Utility.
 *
 * Implements the Euclidean distance calculation (sqrt(dx^2 + dy^2))
 * and the bounding square / rectangular enclosure logic for 2D road paths
 * and terrain grid super-tiles.
 */

export interface Vec2 {
  x: number;
  y: number;
}

export interface BoundingArea {
  xMin: number;
  xMax: number;
  yMin: number;
  yMax: number;
  width: number;
  height: number;
  length: number;
  isSquare: boolean;
  corners: {
    bottomLeft: Vec2;
    bottomRight: Vec2;
    topRight: Vec2;
    topLeft: Vec2;
  };
}

/**
 * Calculates straight-line or multi-segment road length.
 * Sample canonical road: (0,0) to (10,10) -> length = sqrt(200) ≈ 14.1421
 */
export function calculateRoadLength(points: Vec2[]): number {
  if (points.length < 2) return 0;
  let total = 0;
  for (let i = 1; i < points.length; i++) {
    const dx = points[i].x - points[i - 1].x;
    const dy = points[i].y - points[i - 1].y;
    total += Math.hypot(dx, dy);
  }
  return total;
}

/**
 * Obtains the rectangular or equal-sided bounding square enclosure
 * from path or boundary extreme coordinates.
 */
export function deriveBoundingArea(
  points: Vec2[],
  paddingMeters: number = 0,
  forceSquare: boolean = true
): BoundingArea {
  if (points.length === 0) {
    return {
      xMin: 0,
      xMax: 0,
      yMin: 0,
      yMax: 0,
      width: 0,
      height: 0,
      length: 0,
      isSquare: true,
      corners: {
        bottomLeft: { x: 0, y: 0 },
        bottomRight: { x: 0, y: 0 },
        topRight: { x: 0, y: 0 },
        topLeft: { x: 0, y: 0 },
      },
    };
  }

  let minX = Infinity;
  let maxX = -Infinity;
  let minY = Infinity;
  let maxY = -Infinity;

  for (const pt of points) {
    if (pt.x < minX) minX = pt.x;
    if (pt.x > maxX) maxX = pt.x;
    if (pt.y < minY) minY = pt.y;
    if (pt.y > maxY) maxY = pt.y;
  }

  minX -= paddingMeters;
  maxX += paddingMeters;
  minY -= paddingMeters;
  maxY += paddingMeters;

  let width = maxX - minX;
  let height = maxY - minY;

  if (forceSquare && width !== height) {
    const side = Math.max(width, height);
    const cx = (minX + maxX) / 2;
    const cy = (minY + maxY) / 2;
    minX = cx - side / 2;
    maxX = cx + side / 2;
    minY = cy - side / 2;
    maxY = cy + side / 2;
    width = side;
    height = side;
  }

  const length = calculateRoadLength(points);

  return {
    xMin: minX,
    xMax: maxX,
    yMin: minY,
    yMax: maxY,
    width,
    height,
    length,
    isSquare: Math.abs(width - height) < 1e-4,
    corners: {
      bottomLeft: { x: minX, y: minY },
      bottomRight: { x: maxX, y: minY },
      topRight: { x: maxX, y: maxY },
      topLeft: { x: minX, y: maxY },
    },
  };
}

export default deriveBoundingArea;
