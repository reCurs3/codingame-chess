import freetype
from PIL import Image, ImageDraw

colors = ['White', 'Black']
pieces = ['King', 'Queen', 'Rook', 'Bishop', 'Knight', 'Pawn']
face = freetype.Face('chess_merida_unicode.ttf')

def get_glyph(char, outline=0, blend=True):
    face.load_char(char, freetype.FT_LOAD_DEFAULT | freetype.FT_LOAD_NO_BITMAP)
    glyph = face.glyph.get_glyph()
    if outline > 0:
        stroker = freetype.Stroker()
        stroker.set(int(outline * 64), freetype.FT_STROKER_LINECAP_ROUND, freetype.FT_STROKER_LINEJOIN_ROUND, 0)
        glyph.stroke(stroker, True)
    bitmap = glyph.to_bitmap(freetype.FT_RENDER_MODE_NORMAL, freetype.Vector(0,0)).bitmap
    buffer = [x if x == 255 else 0 for x in bitmap.buffer] if not blend else bitmap.buffer
    return Image.frombytes('L', (bitmap.width, bitmap.rows), bytes(buffer), 'raw', 'L', 0, 1)

size = 125
face.set_pixel_sizes(size, size)
for piece in range(6):
    fill = get_glyph(0xe254 + piece, blend=False)
    for color in range(2):
        outline = get_glyph(0x2654 + color*6 + piece)
        for promoted in range(2):
            im = Image.new('RGBA', (size,size), '#00000000')
            draw = ImageDraw.Draw(im)
            draw.bitmap(((size-fill.width)/2, (size-fill.height)/2), fill, '#b90000' if promoted and color == 1 else '#ebeef1')
            draw.bitmap(((size-outline.width)/2, (size-outline.height)/2), outline, '#b90000' if promoted and color == 0 else '#14110e')
            prefix = 'Promoted' if promoted else ''
            im.save(f'../src/main/resources/view/assets/{prefix}{colors[color]}{pieces[piece]}.png', 'png')

size = 48
face.set_pixel_sizes(size, size)
for piece in range(6):
    fill = get_glyph(0xe254 + piece)
    for color in range(2):
        outline = get_glyph(0xe254 + piece, outline=1.5)
        im = Image.new('RGBA', (size,size), '#00000000')
        draw = ImageDraw.Draw(im)
        draw.bitmap(((size-outline.width)/2, (size-outline.height)/2), outline, '#34312e' if color == 0 else '#abaea1')
        draw.bitmap(((size-fill.width)/2, (size-fill.height)/2), fill, '#ebeef1' if color == 0 else '#24211e')
        im.save(f'../src/main/resources/view/assets/Small{colors[color]}{pieces[piece]}.png', 'png')
