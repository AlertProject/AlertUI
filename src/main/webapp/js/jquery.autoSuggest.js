var AutoSuggest = function (fieldId, data, options) {
	var nodes = [];
 	var input = $('#' + fieldId);
 	
 	// fix
	var blurWidth = '200px', normalWidth = '325px';
	$(input).css('width', normalWidth);
	
	var defaults = { 
		asHtmlID: false,
		startText: "Enter Name Here",
		emptyText: "No Results Found",
		limitText: "No More Selections Are Allowed",
		selectedValuesProp: "value", //name of object property
		searchObjProps: "value", //comma separated list of object property names
		queryParam: "q",
		retrieveLimit: false, //number for 'limit' param on ajax request
		extraParams: "",
		matchCase: false,
		minChars: 1,
		keyDelay: 400,
		resultsHighlight: true,
		neverSubmit: false,
		selectionLimit: false,
		showResultList: true,
	  	start: function(){},
	  	selectionClick: function(elem){},
	  	selectionAdded: function(elem){},
	  	selectionRemoved: function(elem){ elem.remove(); },
	  	formatList: false, //callback function
	  	retrieveComplete: function(data){ return data; },
	  	resultClick: function(data){},
	  	formatLabel: function (data) {return data.label;},
	  	resultsComplete: function(){},
	  	addByWrite: true
  	};  
 	var opts = $.extend(defaults, options);
 	
 	var d_type = 'string';
	var req_string = data;
 	
 	if(!opts.asHtmlID){
		x = x+""+Math.floor(Math.random()*100); //this ensures there will be unique IDs on the page if autoSuggest() is called multiple times
		var x_id = "as-input-"+x;
	} else {
		x = opts.asHtmlID;
		var x_id = x;
	}
 	
 	opts.start.call(input);
	input.attr("autocomplete","off").addClass("as-input").attr("id",x_id).attr('placeholder', opts.startText);
	var input_focus = false;
	
	input.wrap('<ul class="as-selections" id="as-selections-'+x+'"></ul>').wrap('<li class="as-original" id="as-original-'+x+'"></li>');
	var selections_holder = $("#as-selections-"+x);
	var org_li = $("#as-original-"+x);				
	var results_holder = $('<div class="as-results" id="as-results-'+x+'"></div>').hide();
	var results_ul =  $('<ul class="as-list"></ul>');
	
	function contains(node) {
		return nodes.indexOf(node) >= 0;
	}
	
	var that = {
		add: function (node) {
			if (!contains(node))
				nodes.push(node);
			
			$(input).removeAttr('placeholder');
			$(input).css('width', blurWidth);
			
			results_ul.html('');
			
			if (node.type == null)
				node.type = 'keyword';
			
			var item = $('<li class="as-selection-item ' + node.type + '"></li>').click(function(){
					opts.selectionClick.call(this, $(this), input);
					selections_holder.children();
					$(this);
				}).mousedown(function(){ input_focus = false; });
			var close = $('<a class="as-close">&times;</a>').click(function(){
				that.remove(node);
				opts.selectionRemoved.call(this, item, node);
				
				if (nodes.length == 0) {
					$(input).attr('placeholder', opts.startText);
					$(input).css('width', normalWidth);
				}
				
				input_focus = true;
				input.focus();
				return false;
			});
			
			org_li.before(item.html(opts.formatLabel(node)).prepend(close));
			opts.selectionAdded.call(this, org_li.prev(), node);
		},
		remove: function (node) {
			var idx = nodes.indexOf(node);
			if (idx >= 0)
				nodes.splice(idx, 1);
		},
		indexOfAttr: function (val, attr) {
			$.each(nodes, function (idx, node) {
				if (node[attr] == val)
					return idx;
			});
			
			return -1;
		},
		
		getNodes: function () {
			return nodes;
		},
		
		getAttrStr: function (attr) {
			var attribute = attr || 'label';
			
			var attrs = [];
			$.each(nodes, function (idx, node) {
				attrs.push(node[attribute]);
			});
			
			return attrs.join();
		},
		
		init: function () {
			selections_holder.click(function(){
				input_focus = true;
				input.focus();
			}).mousedown(function(){ input_focus = false; }).after(results_holder);	

			var timeout = null;
			var prev = "";
			
			// functions
			function keyChange() {
				// ignore if the following keys are pressed: [del] [shift] [capslock]
				if( lastKeyPressCode == 46 || (lastKeyPressCode > 8 && lastKeyPressCode < 32) ){ return results_holder.hide(); }
				var string = input.val().replace(/[\\]+|[\/]+/g,"");
				if (string == prev) return;
				prev = string;
				if (string.length >= opts.minChars) {
					selections_holder.addClass("loading_sugg");
					var limit = "";
					if(opts.retrieveLimit){
						limit = "&limit="+encodeURIComponent(opts.retrieveLimit);
					}
					if(opts.beforeRetrieve){
						string = opts.beforeRetrieve.call(this, string);
					}
					$.getJSON(req_string+"?"+opts.queryParam+"="+encodeURIComponent(string)+limit+opts.extraParams, function(data){ 
						d_count = 0;
						var new_data = opts.retrieveComplete.call(this, data);
						for (k in new_data) if (new_data.hasOwnProperty(k)) d_count++;
						processData(new_data, string); 
					});
				} else {
					selections_holder.removeClass("loading_sugg");
					results_holder.hide();
				}
			}
			
			// TODO
			var num_count = 0;
			function processData(data, query){
				if (!opts.matchCase){ query = query.toLowerCase(); }
				var matchCount = 0;
				results_holder.html(results_ul.html("")).hide();
				for(var i=0;i<d_count;i++){				
					var num = i;
					num_count++;
					var forward = false;
					if(opts.searchObjProps == "value") {
						var str = data[num].value;
					} else {	
						var str = "";
						var names = opts.searchObjProps.split(",");
						for(var y=0;y<names.length;y++){
							var name = $.trim(names[y]);
							str = str+data[num][name]+" ";
						}
					}
					if(str){
						if (!opts.matchCase){ str = str.toLowerCase(); }				
						if(str.search(query) != -1 && that.indexOfAttr(data[num].label, 'label') == -1){
							forward = true;
						}
					}
					if(forward){
						var formatted = $('<li class="as-result-item ' + data[i].type + '" id="as-result-item-'+num+'"></li>').click(function(){
								var raw_data = $(this).data("data");
								var number = raw_data.num;
								if($("#as-selection-"+number, selections_holder).length <= 0){
									var data = raw_data.attributes;
									input.val("").focus();
									prev = "";
									that.add(data);
									opts.resultClick.call(this, raw_data);
									results_holder.hide();
								}
								tab_press = false;
							}).mousedown(function(){ input_focus = false; }).mouseover(function(){
								$("li", results_ul).removeClass("active");
								$(this).addClass("active");
							}).data("data",{attributes: data[num], num: num_count});
						var this_data = $.extend({},data[num]);
						if (!opts.matchCase){ 
							var regx = new RegExp("(?![^&;]+;)(?!<[^<>]*)(" + query + ")(?![^<>]*>)(?![^&;]+;)", "gi");
						} else {
							var regx = new RegExp("(?![^&;]+;)(?!<[^<>]*)(" + query + ")(?![^<>]*>)(?![^&;]+;)", "g");
						}
						
						if(opts.resultsHighlight){
							this_data.label = this_data.label.replace(regx,"<em>$1</em>");
						}
						
						if(!opts.formatList){
							formatted = formatted.html(this_data.label);
						} else {
							formatted = formatted.html(this_data.label);
							formatted = opts.formatList.call(this, this_data, formatted);	
						}
						results_ul.append(formatted);
						delete this_data;
						matchCount++;
						if(opts.retrieveLimit && opts.retrieveLimit == matchCount ){ break; }
					}
				}
				selections_holder.removeClass("loading_sugg");
				if(matchCount <= 0){
					results_ul.html('<li class="as-message">'+opts.emptyText+'</li>');
				}
				results_ul.css("width", selections_holder.outerWidth());
				
				$(results_ul).mousedown(function (event) {
					event.stopPropagation();
					event.preventDefault();
					return false;
				});
				
				results_holder.show();
				opts.resultsComplete.call(this);
			}
			
			function moveSelection(direction){
				if($(":visible",results_holder).length > 0){
					var lis = $("li", results_holder);
					var start = direction == "down" ? lis.eq(0) : lis.filter(":last");
			
					var active = $("li.active:first", results_holder);
					if(active.length > 0){
						if(direction == "down"){
						start = active.next();
						} else {
							start = active.prev();
						}	
					}
					lis.removeClass("active");
					start.addClass("active");
					
					// adjust the scroll
					var list = $('#as-results-'+ x + ' .as-list');
					var listHeight = list.height();
					var activeTop = start.position().top;
					if (activeTop < 0)
						list.scrollTo({top: '-=' + Math.abs(activeTop) + 'px', left:'+=0px'}, 0);
					else if (activeTop + start.outerHeight() > listHeight) {
						var dh = activeTop + start.outerHeight() - listHeight;
						list.scrollTo({top: '+=' + dh + 'px', left:'+=0px'}, 0);
					}
				}
			}
			
			// Handle input field events
			input.focus(function(){	
				$("li.as-selection-item", selections_holder).removeClass("blur");
				if($(this).val() != ""){
					results_ul.css("width",selections_holder.outerWidth());
					results_holder.show();
				}
				input_focus = true;
				return true;
			}).blur(function(){
				if(input_focus){
					$("li.as-selection-item", selections_holder).addClass("blur");
					results_holder.hide();
				}				
			}).keydown(function(e) {
				// track last key pressed
				lastKeyPressCode = e.keyCode;
				first_focus = false;
				switch(e.keyCode) {
				case 38: // up
					e.preventDefault();
					moveSelection("up");
					break;
				case 40: // down
					e.preventDefault();
					moveSelection("down");
					break;
				case 8:  // delete
					if(input.val() == ""){
						var last = nodes.pop();
						selections_holder.children().not(org_li.prev());
						
						opts.selectionRemoved.call(this, org_li.prev(), last);
						
						if (nodes.length == 0) {
							$(input).attr('placeholder', opts.startText);
							$(input).css('width', normalWidth);
						}
					}
					if(input.val().length == 1){
						results_holder.hide();
						prev = "";
					}
					if($(":visible",results_holder).length > 0){
						if (timeout){ clearTimeout(timeout); }
						timeout = setTimeout(function(){ keyChange(); }, opts.keyDelay);
					}
					break;
				case 13: // return
					var active = $("li.active:first", results_holder);
					if(active.length > 0){
						active.click();
						results_holder.hide();
						e.preventDefault();
						e.stopPropagation();
						return false;
					}
					if(opts.neverSubmit || active.length > 0){
						e.preventDefault();
					}
					break;
				default:
					if(opts.showResultList){
						if(opts.selectionLimit && nodes.length >= opts.selectionLimit){
							results_ul.html('<li class="as-message">'+opts.limitText+'</li>');
							results_holder.show();
						} else {
							if (timeout){ clearTimeout(timeout); }
							timeout = setTimeout(function(){ keyChange(); }, opts.keyDelay);
						}
					}
					break;
				}
			});
			
			input.change(function (e) {
				if ($(this).val() != '')
					$(this).removeAttr('placeholder');
			});
			
			
		}
	};
	
	that.init();
	
	return that;
}