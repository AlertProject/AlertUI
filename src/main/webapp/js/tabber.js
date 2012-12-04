(function($){
	$.fn.tabbertab = function () {
		var tabber = this[0];
		$(tabber).removeClass();
		$(tabber).addClass('tabberlive');
		
		// iterate over all the contents and create the navigation
		var ul = $('<ul class="tabbernav">');
		$(tabber).prepend(ul);
		$.each($(tabber).children('.tabbertab'), function (i, tabbertab) {
			// get the title
			var title = $(tabbertab).children('h2').text();
			$(tabbertab).children('h2').remove();
			
			
			var li = $('<li>').append('<a href="" title="' + title + '">' + title + "</a>");
			ul.append(li);
		});
		
		var titles = $('.tabbernav').find('li');
		var contents = $(tabber).children('.tabbertab');
		
		/*
		 * Selects the tab specified by the index
		 */
		function select(idx) {
			titles.removeClass('tabberactive');
			$(titles[idx]).addClass('tabberactive');
		
			contents.addClass('tabbertabhide');
			$(contents[idx]).removeClass('tabbertabhide');
		}
		
		titles.click(function (event) {
			// first set the active title and remember the index
			var selectedIdx = -1;
			for (var i = 0; i < titles.length; i++) {
				if (titles[i] == this) {
					selectedIdx = i;
					break;
				}
			}
			
			select(selectedIdx);
			return false;
		});
		
		// select the first tab
		select(0);
	};
})(jQuery);