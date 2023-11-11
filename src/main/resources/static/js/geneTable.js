
function GenesTable(parentDiv,worldId){
    this.__parentDiv = parentDiv;
    this.__worldId = worldId;
}

GenesTable.prototype.viewOrganismDetails = function(organismId,onSuccess,onFailure){

    $.ajax( {
        url: "/genetics/v1.0/" + this.__worldId + "/inspect/" + organismId,
        type: "GET",
        contentType: "application/json",
        dataType: "json"
    })
    .done( msg => {
        this._drawTable(msg);
        if( onSuccess && onSuccess instanceof Function ){
            onSuccess(this);
        }
    })
    .fail(function(msg) {
        console.log( "failed to get organism's details: " + msg );
        if( onFailure && onFailure instanceof Function ){
            onFailure(this,msg);
        }
    });
}

GenesTable.prototype._drawTable = function(msg){
    if( msg && msg.organism ){
        let overview = document.createElement("div");
        overview.className += " organism-overview";
        this.__parentDiv.html("");
        this.__parentDiv.append( overview );

        let nameElement = document.createElement("div");
        overview.append( nameElement );
        $(nameElement).html( "Name: <b>" + msg.organism.name + "</b>");

        let genusElement = document.createElement("div");
        overview.append( genusElement );
        $(genusElement).html( "Genus: <b>" + msg.organism.genus+ "</b>" );
        //Name, Genus, Parent

        let parentElement = document.createElement("div");
        overview.append( parentElement );
        if( msg.organism.parentId ){
            $(parentElement).html( "Parent: <b>" + msg.organism.parentId+ "</b>" );
        } else {
            $(parentElement).html( "Parent: <b>GOD</b>");
        }

        if( msg.organism.genes ){
            for( let i = 0; i < msg.organism.genes.length; ++i){
                let gene = msg.organism.genes[i];
                console.log( "Gene " + i + ": " + gene.nucleotideA + ", " + gene.nucleotideB + ", "
                 + gene.nucleotideC + ", " + gene.nucleotideD );
                 let geneElement = document.createElement("div");
                 geneElement.className += " gene";
                 this.__parentDiv.append( geneElement );

                 let nA = document.createElement("div");
                 nA.className += " nucleoA";
                 let nB = document.createElement("div");
                 nB.className += " nucleoB";
                 let nC = document.createElement("div");
                 nC.className += " nucleoC";
                 let nD = document.createElement("div");
                 nD.className += " nucleoD";

                 geneElement.append(nA);
                 geneElement.append(nB);
                 geneElement.append(nC);
                 geneElement.append(nD);

                 $(nA).html(gene.nucleotideA);
                 $(nB).html(gene.nucleotideB);
                 $(nC).html(gene.nucleotideC);
                 $(nD).html(gene.nucleotideD);

            }
        }
        let postview = document.createElement("div");
        postview.className += " organism-postview";
        this.__parentDiv.append( postview );
        $(postview).html(msg.genome);
    } else {
        console.log( "Unable to read genes: " + msg);
    }
}