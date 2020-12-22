import { WIDTH, HEIGHT } from './core/constants.js'
var OFFSET = 40;

export const Constants = {
    Columns: 8,
    Rows: 8,

    Offset: OFFSET,
    SquareSize: (HEIGHT - OFFSET * 2) / 8,
    CaptureSize: 48,
}
