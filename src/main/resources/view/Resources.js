import { Pool } from './Pool.js';

export class Resources {
    init() {
        var textureNames = [
            'WhitePawn.png', 'WhiteKnight.png', 'WhiteBishop.png', 'WhiteRook.png', 'WhiteQueen.png', 'WhiteKing.png',
            'BlackPawn.png', 'BlackKnight.png', 'BlackBishop.png', 'BlackRook.png', 'BlackQueen.png', 'BlackKing.png',
        ];
        this.textures = textureNames.map(x => PIXI.Texture.from(x));
        this.texturesSmall = textureNames.map(x => PIXI.Texture.from('Small'+x));
        this.texturesPromoted = textureNames.map(x => PIXI.Texture.from('Promoted'+x));
        this.pieces = 'PNBRQKpnbrqk';

        this.sprites = new Pool(() => new PIXI.Sprite());
    }

    close() {
        
    }

    getTexture(piece, type) {
        var index = this.pieces.indexOf(piece);
        if (index == -1) return;
        return (type == 'small' ? this.texturesSmall : (type == 'promoted' ? this.texturesPromoted : this.textures))[index];
    }

    acquireSprite(piece, type) {
        var sprite = this.sprites.acquire();
        sprite.texture = this.getTexture(piece, type);
        return sprite;
    }

    releaseSprite(sprite) {
        this.sprites.release(sprite)
    }
}
