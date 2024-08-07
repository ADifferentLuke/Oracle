/*
 * (c) 2023 Luke McOmber
 * This code is licensed under MIT license (see LICENSE.txt for details)
 */

function Display(   id,detailsView, dashboard,
                    totalTicksDisplay, totalDaysDisplay, currentTickDisplay, domCanvasElement,
                    onSuccess, onFailure, onEnd ) {
    this.__id = id;
    this.__canvas = domCanvasElement;
    this.__dashboard = dashboard;
    this.__onEnd = onEnd;
    this.__onSuccess = onSuccess;
    this.__onFailure = onFailure;

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

    this.resize(onSuccess,onFailure,onEnd);



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
                url: "/genetics/v1.0/" + this.__id + "/inspect",
                type: "GET",
                contentType: "application/json",
                dataType: "json",
                data: {
                    "x" : xcell,
                    "y" : ycell,
                    "z" : 0
                }
            })
            .done( msg => {
                console.log(msg);

                for( var key in msg ){
                    if( key == "occupant") {
                    //TODO this can be condensed - lots of duplicate code
                        this.__detailsView.viewOrganismDetails(msg.occupant.organism,null,null);
                        for( occKey in msg.occupant ){
                            let keyDiv = document.createElement("div");
                            let valDiv = document.createElement("div");

                            $(keyDiv).html(_.capitalize(occKey));
                            $(valDiv).html(msg.occupant[occKey]);

                            this.__dashboard.append( keyDiv );
                            this.__dashboard.append( valDiv );
                        }
                    } else if( key == "environment") {
                        for( envKey in msg.environment ){
                            let keyDiv = document.createElement("div");
                            let valDiv = document.createElement("div");

                            $(keyDiv).html(_.capitalize(envKey));
                            $(valDiv).html(msg.environment[envKey]);

                            this.__dashboard.append( keyDiv );
                            this.__dashboard.append( valDiv );
                        }
                    } else if( key == "coordinates") {
                        let coordStr = "(" + msg.coordinates.xAxis + "," +
                        msg.coordinates.yAxis + "," +
                        msg.coordinates.zAxis + ")";

                        let keyDiv = document.createElement("div");
                        let valDiv = document.createElement("div");
                        $(keyDiv).html(_.capitalize(key));
                        $(valDiv).html(coordStr);

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

Display.prototype.resize = function(onSuccess,onFailure,onEnd) {

    //stop refresh thread
    if( this.__refreshInterval ){
        clearInterval( this.__refreshInterval );
        this.__refreshInterval = null;
    }

    $.ajax( {
        url: "/genetics/v1.0/" + this.__id,
        type: "GET",
        contentType: "application/json",
        dataType: "json"
    })
    .done( msg => {
        this._calculateNewSize(msg.result,onSuccess,onFailure,onEnd);
    })
    .fail(function(msg) {
        console.log( "failed to create world on resize.");
        if( onFailure && onFailure instanceof Function ){
            onFailure(this,msg.result);
        }
    });
}

Display.prototype._calculateNewSize = function(msg, onSuccess, onFailure, onEnd){
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
                this.draw(onSuccess,onFailure,onEnd);
                if( ! this.__refreshInterval ){
                    this.__refreshInterval = setInterval( this._draw, 500, this );
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
        url: "/genetics/v1.0/" + this.__id + "/advance",
        type: "GET",
        contentType: "application/json",
        dataType: "json",
        data: {
            "turns" : turns
        }
    })
    .done( msg => {
        if( onSuccess && onSuccess instanceof Function ){
            onSuccess(this,msg);
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
Display.prototype.draw = function(onSuccess,onFailure,onEnd) {
    this._draw(this);
}

//Called repeatedly
Display.prototype._draw = function(display){
    $.ajax( {
        url: "/genetics/v1.0/" + display.__id + "/ecology",
        type: "GET",
        contentType: "application/json",
        dataType: "json"
    })
    .done( msg => {
        display._drawPixels(msg);
        if( ! msg.active ){
            //clear poller if theres no more activity
            clearInterval( display.__refreshInterval );
            if( display.__onEnd && display.__onEnd instanceof Function ){
                display.__onEnd(display,msg);
            }
        }
        if( display.__onSuccess && display.__onSuccess instanceof Function ){
            display.__onSuccess(display,msg);
        }
    })
    .fail(function(msg) {
        if( display.__onFailure && display.__onFailure instanceof Function ){
            display.__onFailure(display,msg);
        }
    });
}

Display.prototype.clear = function(){
        let ctx = this.__canvas.getContext("2d");
        ctx.clearRect(0,0,this.__canvas.width,this.__canvas.height);
}

Display.prototype._drawPixels = function(msg){
    //console.log(msg);
    this.clear();

    if( msg.totalTicks ){
        this.__totalTicksDisplay.html( msg.totalTicks );
    }
    if( msg.totalDays ){
        this.__totalDaysDisplay.html( msg.totalDays );
    }
    if( msg.currentTick ){
        this.__currentTickDisplay.html( msg.currentTick );
    }

    if( msg.organisms && 0 < msg.organisms.length){
        for( let i = 0; msg.organisms.length > i; ++i){
            let organism = msg.organisms[i];
            for( let j = 0; j < organism.cells.length; ++j){
                    let cell = organism.cells[j];
                    this.__drawCell(cell,organism.alive);
            }
        }
    }
}
Display.prototype.__drawCell = function(cell,isAlive){

    if( cell ){
        let x = cell.x * this.__pixelWidth;
        let y = cell.y * this.__pixelHeight;

        let ctx = this.__canvas.getContext("2d");
        if( isAlive ) {
        //TODO color shold be moved server side
            if( 'seed' == cell.type ){
                ctx.fillStyle = "black";
            } else if( 'leaf' == cell.type ){
                ctx.fillStyle = "green";
            } else if( 'root' == cell.type ){
                ctx.fillStyle = "brown";
            } else if( 'stem' == cell.type ){
                ctx.fillStyle = "purple";
            }  else {
                ctx.fillStyle = "black";
            }
        } else {
            if( 'seed' == cell.type && !cell.activated ){
                ctx.fillStyle= "black";
            } else {
                ctx.fillStyle="gray";
            }
        }
        ctx.fillRect( x, this.__canvas.height - y - this.__pixelHeight, this.__pixelWidth, this.__pixelHeight);
        //console.log( "Set pixel: [" + x + "," + (this.__canvas.height - y - this.__pixelHeight) + "]");
    }
}
