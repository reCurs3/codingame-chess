import { WIDTH, HEIGHT } from './core/constants.js'
import { Constants } from './Constants.js'
import { Resources } from './Resources.js'

export class ChessViewerModule {
    static get name () {
        return 'chess'
    }

    constructor () {
        this.resources = new Resources();
    }

    handleFrameData (frameInfo, frameData) {
        frameData.frame = frameInfo.number;
        var parts = frameData.fen.split(' ');

        // Read FEN board and store pieces.
        var board = parts[0];
        frameData.pieces = []
        const pieceChars = 'pnbrq';
        var pieceCounts = [0,0,0,0,0];
        var reserves = [[0,0,0,0,0],[0,0,0,0,0]];
        var column = 0, row = Constants.Rows - 1;
        for (var c of board) {
            if (c == '/') {
                row--;
                column = 0;
            }
            else if (c.charCodeAt(0) >= '1'.charCodeAt(0) && c <= '9'.charCodeAt(0))
                column += c.charCodeAt(0) - '0'.charCodeAt(0);
            else if (c == '~')
                frameData.pieces[frameData.pieces.length - 1].promoted = true;
            else {
                var pieceIndex = pieceChars.indexOf(c.toLowerCase());
                if (row >= 0 && row < Constants.Rows && column >= 0 && column < Constants.Columns) {
                    frameData.pieces.push({
                        pieceChar: c,
                        column: column,
                        row: row
                    });
                    if (pieceIndex != -1)
                        pieceCounts[pieceIndex] += c == c.toUpperCase() ? 1 : -1;
                } else if (row < 0 && pieceIndex != -1)
                    reserves[c == c.toUpperCase() ? 0 : 1][pieceIndex]++;
                column++;
            }
        }

        frameData.color = parts[1] == 'w' ? 'b' : 'w';
        frameData.castling = parts[2];
        frameData.fiftyMove = parts[4]/2;
        frameData.moveText = frameData.move ? ((parts[5] - (frameData.color == 'w' ? 0 : 1)) + (frameData.color == 'w' ? '.' : '..') + ' ' + frameData.move) : '';

        // Assemble captured pieces string for display.
        frameData.captured = ['',''];
        for (var i = 0; i < 5; i++) {
            if (!this.globalData.crazyHouse) {
                if (pieceCounts[i] > 0)
                    frameData.captured[0] += pieceChars[i].repeat(pieceCounts[i]);
                else if (pieceCounts[i] < 0)
                    frameData.captured[1] += pieceChars[i].toUpperCase().repeat(-pieceCounts[i]);
            } else {
                for (var c = 0; c < 2; c++) {
                    var ch = pieceChars[i];
                    if (c == 0) ch = ch.toUpperCase();
                    frameData.captured[c] += ch.repeat(reserves[c][i]);
                }
            }
        }

        // Update highlighted squares to display current move.
        var highlights = frameData.highlights;
        frameData.highlights = [];
        if (highlights) for (var squareStr of highlights) {
            if (squareStr.length != 2) continue;
            var column = squareStr.charCodeAt(0) - 'a'.charCodeAt(0);
            var row = squareStr.charCodeAt(1) - '1'.charCodeAt(0);
            frameData.highlights.push([column, row]);
        }

        this.frames.push(frameData);
        return frameData;
    }

    handleGlobalData (players, globalData) {
        this.players = players;
        this.globalData = globalData;
        this.frames = []

        // Add FEN and PGN strings to options panel for copy/paste convenience.
        var settings = document.getElementsByClassName('settings_panel_form')[0];
        var createOption = (headerName, elementName, id) => {
            var element = document.getElementById(id);
            if (element) return element;

            var option = document.createElement('div');
            option.className = 'settings_option';
            var header = document.createElement('h3');
            header.innerText = headerName;
            option.appendChild(header);
            element = document.createElement(elementName);
            element.className = 'settings_button';
            element.id = id;
            element.style.width = '100%';
            element.style.background = 'transparent';
            element.style.color = '#b3b9ad';
            element.style.padding = '3px';
            element.readOnly = true;
            element.onfocus = function() { this.select(); };
            element.onfocusout = function() { this.selectionStart = this.selectionEnd; };
            option.appendChild(element);
            settings.appendChild(option);
            return element;
        };

        this.fenInput = createOption('FEN', 'input', 'chess_fen');
        this.fenInput.type = 'text';

        this.pgnArea = createOption('PGN', 'textarea', 'chess_pgn');
        this.pgnArea.style.resize = 'none';
        this.pgnArea.style.height = '200px';
        this.pgnArea.style.whiteSpace = 'pre';
        this.pgnArea.value = '';
    }

    updateScene (previousData, currentData, progress, speed) {
        // Only update when frame has actually changed.
        var frameData = progress == 1 ? currentData : previousData;
        if (this.frameNumber === frameData.frame) return;
        this.frameNumber = frameData.frame;
        this.frameData = frameData;

        // Cleanup previous frame.
        if (this.highlights)
            this.container.removeChild(this.highlights);
        if (this.sprites) for (var sprite of this.sprites) {
            if (sprite.container) {
                sprite.container.removeChild(sprite);
                delete sprite.container;
            }
            this.resources.releaseSprite(sprite);
        }
        this.sprites = []

        // Highlighted squares for last move.
        this.highlights = new PIXI.Graphics();
        for (var highlight of frameData.highlights) {
            var [column, row] = highlight;
            this.highlights.beginFill(0xf2bb13, 0.5)
            this.highlights.drawRect(Constants.Offset + column * Constants.SquareSize, Constants.Offset + (Constants.Rows - 1 - row) * Constants.SquareSize, Constants.SquareSize, Constants.SquareSize)
        }
        this.container.addChild(this.highlights);

        // Put pieces on board.
        for (var piece of frameData.pieces) {
            var sprite = this.resources.acquireSprite(piece.pieceChar, piece.promoted ? 'promoted' : '');
            if (sprite != null) {
                sprite.x = Constants.Offset + piece.column * Constants.SquareSize;
                sprite.y = Constants.Offset + (Constants.Rows - 1 - piece.row) * Constants.SquareSize;
                sprite.width = Constants.SquareSize;
                sprite.height = Constants.SquareSize;
                sprite.container = this.container;
                this.container.addChild(sprite);
                this.sprites.push(sprite);
            }
        }

        // Update fields on right panel.
        this.moveText.style.fill = frameData.color == 'w' ? '#ebeef1' : '#abaea1';
        this.moveText.text = frameData.moveText;
        this.castlingText.text = frameData.castling;
        this.fiftyMoveText.text = frameData.fiftyMove;
        this.gameText.text = frameData.game + 1;
        this.statusText.text = frameData.status;
        this.commentText.text = frameData.comment;

        // Update players info panel.
        for (var i = 0; i < 2; i++) {
            var y = i == 0 ? HEIGHT - Constants.Offset - Constants.SquareSize : Constants.Offset;
            var playerIdx = frameData.game == 0 ? i : 1 - i;
            this.playerContainers[playerIdx].y = y;
            var style = this.playerNames[playerIdx].style;
            style.fill = i == 0 ? '#ebeef1' : '#24211e';
            style.stroke = i == 0 ? '#34312ee0' : '#abaea1e0';
            style.strokeThickness = i == 0 ? 10 : 6;

            var captured = this.playerCaptureds[playerIdx];
            var captureX = 0;
            var lastPiece;
            for (var piece of frameData.captured[i]) {
                var sprite = this.resources.acquireSprite(piece, 'small');
                if (sprite != null) {
                    if (piece == lastPiece) captureX -= Constants.CaptureSize/2 + 5;
                    sprite.x = captureX;
                    sprite.y = 0;
                    sprite.width = Constants.CaptureSize;
                    sprite.height = Constants.CaptureSize;
                    sprite.container = captured;
                    captured.addChild(sprite);
                    this.sprites.push(sprite);

                    captureX += Constants.CaptureSize;
                }
                lastPiece = piece;
            }

            var scoreText = '';
            if (frameData.scores[i] > 1) scoreText += Math.floor(frameData.scores[i] / 2);
            if (frameData.scores[i] % 2 == 1) scoreText += '½';
            if (!scoreText) scoreText = '0';
            this.playerScores[i].text = scoreText;
        }

        // Update FEN string in settings panel.
        this.fenInput.value = frameData.fen;
    }

    reinitScene (container, canvasData) {
        // Cleanup.
        this.resources.close();
        delete this.frameNumber;
        delete this.highlights;
        delete this.sprites;

        // Reinit.
        this.container = container;
        this.resources.init();

        // Draw chess board.
        var background = new PIXI.Graphics();
        background.beginFill(0x0d1015);
        background.drawRect(0, 0, WIDTH, HEIGHT);
        for (var row = 0; row < Constants.Rows; row++) {
            for (var column = 0; column < Constants.Columns; column++) {
                var light = (column ^ row) & 1;
                background.beginFill(light ? 0xdee3e6 : 0x8ca2ad);
                background.drawRect(Constants.Offset + column * Constants.SquareSize, Constants.Offset + (Constants.Rows - 1 - row) * Constants.SquareSize, Constants.SquareSize, Constants.SquareSize);
            }
        }
        container.addChild(background);

        // Column/row labels.
        for (var column = 0; column < Constants.Columns; column++) {
            var text = new PIXI.Text(String.fromCharCode('a'.charCodeAt(0) + column) + ' ', new PIXI.TextStyle({
                fontSize: 25,
                fill: '#ebeef1',
                stroke: '#14110e',
                strokeThickness: 3,
                lineJoin: 'round',
            }));
            text.anchor.set(0.5, 0);
            text.x = Constants.Offset + Constants.SquareSize * (column + 0.5);
            text.y = HEIGHT - Constants.Offset;
            container.addChild(text);
        }
        for (var row = 0; row < Constants.Rows; row++) {
            var text = new PIXI.Text(String.fromCharCode('1'.charCodeAt(0) + row) + ' ', new PIXI.TextStyle({
                fontSize: 25,
                fill: '#ebeef1',
                stroke: '#14110e',
                strokeThickness: 3,
                lineJoin: 'round',
            }));
            text.anchor.set(1.0, 0.5);
            text.x = Constants.Offset;
            text.y = HEIGHT - Constants.Offset - Constants.SquareSize * (row + 0.5);
            container.addChild(text);
        }

        // Player info panels.
        var rightX = Constants.Offset*2 + Constants.SquareSize * Constants.Columns;
        this.playerContainers = []
        this.playerNames = []
        this.playerCaptureds = []
        this.playerScores = []
        for (var i = 0; i < 2; i++) {
            var player = this.players[i];
            var playerContainer = new PIXI.Graphics();
            playerContainer.x = rightX;
            container.addChild(playerContainer);
            this.playerContainers.push(playerContainer);

            playerContainer.beginFill(player.color);
            playerContainer.drawRect(-10, 0, 4, Constants.SquareSize);
            playerContainer.beginFill(player.color, 0.04);
            playerContainer.drawRect(0, 0, WIDTH - Constants.Offset - playerContainer.x, Constants.SquareSize);

            var avatar = new PIXI.Sprite(player.avatar);
            avatar.width = avatar.height = 116;
            avatar.y = (Constants.SquareSize - avatar.height) / 2;
            playerContainer.addChild(avatar);

            var name = new PIXI.Text(player.name, new PIXI.TextStyle({
                fontSize: 50,
                fontWeight: 900,
                lineJoin: 'round',
            }));
            name.x = avatar.x + avatar.width + 10;
            name.y = avatar.y;
            playerContainer.addChild(name);
            this.playerNames.push(name);

            var capturedContainer = new PIXI.Container();
            capturedContainer.x = name.x + 2;
            capturedContainer.y = name.y + name.height + 10;
            this.playerCaptureds.push(capturedContainer);
            playerContainer.addChild(capturedContainer);

            var score = new PIXI.Text('0', new PIXI.TextStyle({
                fontSize: 35,
                fill: player.color
            }));
            score.x = WIDTH - Constants.Offset - playerContainer.x - 20;
            score.y = Constants.SquareSize / 2;
            score.anchor.set(1.0, 0.5);
            this.playerScores.push(score);
            playerContainer.addChild(score);
        }

        // Right panel fields.
        var headerStyle = new PIXI.TextStyle({
            fontSize: 25,
            fontWeight: 700,
            fill: '#f2bb13'
        });
        var fieldStyle = new PIXI.TextStyle({
            fontSize: 35,
            fill: '#ebeef1'
        });
        this.moveHeaderText = new PIXI.Text('CURRENT MOVE', headerStyle);
        this.moveHeaderText.x = rightX;
        this.moveHeaderText.y = Constants.Offset + Constants.SquareSize * 2.5 + 16;
        container.addChild(this.moveHeaderText);

        this.moveText = new PIXI.Text('', new PIXI.TextStyle({
            fontSize: 35
        }));
        this.moveText.x = this.moveHeaderText.x;
        this.moveText.y = this.moveHeaderText.y + 40;
        container.addChild(this.moveText);

        this.castlingHeaderText = new PIXI.Text('CASTLING', headerStyle);
        this.castlingHeaderText.x = this.moveHeaderText.x + 250;
        this.castlingHeaderText.y = this.moveHeaderText.y;
        container.addChild(this.castlingHeaderText);

        this.castlingText = new PIXI.Text('', fieldStyle);
        this.castlingText.x = this.castlingHeaderText.x;
        this.castlingText.y = this.castlingHeaderText.y + 40;
        container.addChild(this.castlingText);

        this.fiftyMoveHeaderText = new PIXI.Text('FIFTY-MOVE', headerStyle);
        this.fiftyMoveHeaderText.x = this.castlingHeaderText.x + 200;
        this.fiftyMoveHeaderText.y = this.moveHeaderText.y;
        container.addChild(this.fiftyMoveHeaderText);

        this.fiftyMoveText = new PIXI.Text('', fieldStyle);
        this.fiftyMoveText.x = this.fiftyMoveHeaderText.x;
        this.fiftyMoveText.y = this.fiftyMoveHeaderText.y + 40;
        container.addChild(this.fiftyMoveText);

        this.gameHeaderText = new PIXI.Text('GAME', headerStyle);
        this.gameHeaderText.x = this.fiftyMoveHeaderText.x + 200;
        this.gameHeaderText.y = this.moveHeaderText.y;
        container.addChild(this.gameHeaderText);

        this.gameText = new PIXI.Text('', fieldStyle);
        this.gameText.x = this.gameHeaderText.x;
        this.gameText.y = this.gameHeaderText.y + 40;
        container.addChild(this.gameText);

        this.statusHeaderText = new PIXI.Text('STATUS', headerStyle);
        this.statusHeaderText.x = rightX;
        this.statusHeaderText.y = Constants.Offset + Constants.SquareSize * 3.5 + 16;
        container.addChild(this.statusHeaderText);

        this.statusText = new PIXI.Text('', new PIXI.TextStyle({
            fontSize: 30,
            fill: '#ebeef1',
            lineHeight: 40
        }));
        this.statusText.x = this.statusHeaderText.x;
        this.statusText.y = this.statusHeaderText.y + 40;
        container.addChild(this.statusText);

        this.commentHeaderText = new PIXI.Text('COMMENT', headerStyle);
        this.commentHeaderText.x = rightX;
        this.commentHeaderText.y = Constants.Offset + Constants.SquareSize * 4.5 + 16;
        container.addChild(this.commentHeaderText);

        this.commentText = new PIXI.Text('', new PIXI.TextStyle({
            fontSize: 30,
            fill: '#ebeef1',
            lineHeight: 40
        }));
        this.commentText.x = this.commentHeaderText.x;
        this.commentText.y = this.commentHeaderText.y + 40;
        container.addChild(this.commentText);

        // Create PGN text only once here since there is no better place to do so.
        if (!this.pgnArea.value) {
            // First pass to get individual game results.
            var results = []
            for (var frame of this.frames) {
                if (frame.result)
                    results.push(frame.result);
            }

            // Second pass to actually print the PGN.
            var pgn = '';
            var game = -1;
            var startPosition = this.frames[0].fen;
            var halfMove = 0;
            for (var frame of this.frames) {
                if (game != frame.game) {
                    if (game >= 0) pgn += ' ' + results[game] + '\n\n';
                    game++;
                    halfMove = 0;
                    pgn += '[Event "Codingame Chess Bot Game"]\n';
                    pgn += '[Site "https://www.codingame.com/"]\n';
                    pgn += '[Date "??"]\n';
                    pgn += '[Round "??"]\n';
                    pgn += '[White "' + this.players[game].name.replace('"', '\\"') + '"]\n';
                    pgn += '[Black "' + this.players[1-game].name.replace('"', '\\"') + '"]\n';
                    pgn += '[Result "' + results[game] + '"]\n';
                    if (this.globalData.crazyHouse) pgn += '[Variant "Crazyhouse"]\n';
                    pgn += '[FEN "' + startPosition + '"]\n';
                    pgn += '\n';
                }
                if (!frame.move) continue;

                if (halfMove % 2 == 0) {
                    if (halfMove > 0) pgn += '\n';
                    pgn += (1 + Math.floor(halfMove / 2)) + '.';
                }
                pgn += ' ' + frame.move;
                halfMove++;
            }
            pgn += ' ' + results[game];
            this.pgnArea.value = pgn;
        }
    }

    animateScene (delta) {
    }
}
