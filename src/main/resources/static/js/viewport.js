
function Display(   id,detailsView, dashboard,
                    totalTicksDisplay, totalDaysDisplay, currentTickDisplay, domCanvasElement,
                    onSuccess, onFailure ) {
    this.__id = id;
    this.__canvas = domCanvasElement;
    this.__dashboard = dashboard;

    //TODO how do I vierfy detailsPane is a GeneTable
    this.__detailsView = detailsView;

    this.__totalTicksDisplay = totalTicksDisplay;
    this.__totalDaysDisplay = totalDaysDisplay;
    this.__currentTickDisplay = currentTickDisplay;

    this.__totalTicksDisplay.html(0);
    this.__totalDaysDisplay.html(0);
    this.__currentTickDisplay.html(0);

    this.__pixelWidth = -1;
    this.__pixelHeight = -1;

    this.resize(onSuccess,onFailure);



    this.__canvas.addEventListener('mousedown', event => {
        this._onClick(event);
    });

}

Display.prototype._onClick = function(event){

        let rect = this.__canvas.getBoundingClientRect();

        const x = event.clientX - rect.left;
        //const y =  (event.clientY - rect.top) - this.__pixelHeightOffset;
        const y =  this.__canvas.height - (event.clientY - rect.top);

        const xcell = Math.floor(x / this.__pixelWidth);
        const ycell = Math.floor(y / this.__pixelHeight);

        $(this.__dashboard).html("");

        //Get information on location

        console.log("x: " + x + " y: " + y);
        console.log("x-cell: " + xcell + " y-cell: " + ycell);

            $.ajax( {
                url: "/genetics/v1.0/world/" + this.__id + "/inspect",
                type: "GET",
                contentType: "application/json",
                dataType: "json",
                data: {
                    "x" : xcell,
                    "y" : ycell
                }
            })
            .done( msg => {
                console.log(msg);

                for( var key in msg ){
                    if( key == "organism") {
                        this.__detailsView.viewOrganismDetails(msg.organism,null,null);
                    } else {
                        //Create two divs
                       let keyDiv = document.createElement("div");
                       let valDiv = document.createElement("div");

                       $(keyDiv).html(_.capitalize(key));
                       $(valDiv).html(msg[key]);

                       this.__dashboard.append( keyDiv );
                       this.__dashboard.append( valDiv );
                    }
                }
            })
            .fail(function(msg) {
                console.log( "Unable to inspect location");
                if( onFailure && onFailure instanceof Function ){
                    onFailure(this,msg);
                }
            });
}

Display.prototype.resize = function(onSuccess,onFailure) {

    //stop refresh thread
    if( this.__refreshInterval ){
        clearInterval( this.__refreshInterval );
        this.__refreshInterval = null;
    }

    $.ajax( {
        url: "/genetics/v1.0/world/" + this.__id,
        type: "GET",
        contentType: "application/json",
        dataType: "json"
    })
    .done( msg => {
        this._calculateNewSize(msg,onSuccess,onFailure);
    })
    .fail(function(msg) {
        console.log( "failed to create world on resize.");
        if( onFailure && onFailure instanceof Function ){
            onFailure(this,msg);
        }
    });
}

Display.prototype._calculateNewSize = function(msg, onSuccess, onFailure){
    console.log("_calculateNewSize(" + msg + ")");
    if(msg){
        let x = msg.width;
        let y = msg.height;
        let z = msg.depth;


        if(this.__canvas){
           let absoluteX = this.__canvas.scrollWidth;
           let absoluteY = this.__canvas.scrollHeight;
           this.__canvas.width = absoluteX;
           this.__canvas.height = absoluteY;

           console.log( "Viewport size [" + absoluteX + "x" + absoluteY + "]");
           console.log( "Canvas size [" + this.__canvas.width + "," + this.__canvas.height + "]");

           if( absoluteX >= x && absoluteY >= y ){
                this.__pixelWidth = Math.floor(absoluteX / x);
                this.__pixelHeight = Math.floor(absoluteY / y);

                /*
                 * Because we invert the y-axis, the left over pixels that don't fit in the environment
                 *   begin at offset 0 instead of at offset end
                 */
                this.__pixelHeightOffset = ( absoluteY - ( this.__pixelHeight * y ));

                console.log( "Leftover height: " + this.__pixelHeightOffset );

                console.log( "Pixels will be [" + this.__pixelWidth + "x" + this.__pixelHeight + "]");
                this.draw(onSuccess,onFailure);
                if( ! this.__refreshInterval ){
                    this.__refreshInterval = setInterval( this._draw, 1000, this );
                }

           } else {
            if( onFailure && onFailure instanceof Function ){
                onFailure( this,"View port is too small to show world.");
            } else {
                console.log( "Display::_calculateNewSize - View port is too small to show world");
            }
           }
        } else {
            if( onFailure && onFailure instanceof Function ){
                onFailure( this,"HTML5 Canvas object not defined,");
            } else {
                console.log( "Display::_calculateNewSize - HTML5 Canvas object not defined,");
            }
        }
    }
}
Display.prototype.advance = function(turns,onSuccess,onFailure){

    $.ajax( {
        url: "/genetics/v1.0/world/advance/" + this.__id,
        type: "GET",
        contentType: "application/json",
        dataType: "json",
        data: {
            "turns" : turns
        }
    })
    .done( msg => {
        if( onSuccess && onSuccess instanceof Function ){
            onSuccess(this);
        }
    })
    .fail(msg => {
        if( onFailure && onFailure instanceof Function ){
            onFailure(this,msg);
        } else {
            console.log( "Failed to advance world: " + msg );
        }
    });

}
Display.prototype.draw = function(onSuccess,onFailure) {
    this._draw(this,onSuccess,onFailure);
}

//This needs to be static context because it is called from setInterval
Display.prototype._draw = function(display,onSuccess,onFailure){
    $.ajax( {
        url: "/genetics/v1.0/world/details/" + display.__id,
        type: "GET",
        contentType: "application/json",
        dataType: "json"
    })
    .done( msg => {
        display._drawPixels(msg);
        if( onSuccess && onSuccess instanceof Function ){
            onSuccess(display);
        }
    })
    .fail(function(msg) {
        if( onFailure && onFailure instanceof Function ){
            onFailure(display,msg);
        }
    });
}

Display.prototype._drawPixels = function(msg){
    //console.log(msg);

    if( msg.totalTicks ){
        this.__totalTicksDisplay.html( msg.totalTicks );
    }
    if( msg.totalDays ){
        this.__totalDaysDisplay.html( msg.totalDays );
    }
    if( msg.currentTick ){
        this.__currentTickDisplay.html( msg.currentTick );
    }

    if( msg.cells && 0 < msg.cells.length){
        for( let i = 0; i < msg.cells.length; ++i){
            let cell = msg.cells[i];
            this.__drawCell(cell);
        }
    }
}
Display.prototype.__drawCell = function(cell){

    if( cell ){
        let x = cell.x * this.__pixelWidth;
        let y = cell.y * this.__pixelHeight;

        let ctx = this.__canvas.getContext("2d");
        //TODO color shold be moved server side
        if( 'seed' == cell.type ){
            ctx.fillStyle = "red";
        } else if( 'leaf' == cell.type ){
            ctx.fillStyle = "green";
        } else if( 'root' == cell.type ){
            ctx.fillStyle = "brown";
        } else if( 'stem' == cell.type ){
            ctx.fillStyle = "purple";
        } else {
            ctx.fillStyle = "black";
        }
        ctx.fillRect( x, this.__canvas.height - y - this.__pixelHeight, this.__pixelWidth, this.__pixelHeight);
        console.log( "Set pixel: [" + x + "," + (this.__canvas.height - y - this.__pixelHeight) + "]");
    }
}
