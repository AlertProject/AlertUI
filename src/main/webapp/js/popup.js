(function($){
	$.fn.popup = function (config) {
		var popup = this[0];
		var trigger = config.trigger;
		var align = config.align || 'center';
		var fadeDuration = config.duration || 'fast';
		var verticalOffset = config.top == null ? 2 : config.top;
		var horizontalOffset = config.left == null ? 0 : config.left;
		var eventType = config.event || 'click';
		var beforeOpen = config.beforeOpen;
		var beforeClose = config.beforeClose;
		
		var arrow = $($(popup).children('.arrow'));
		var arrowBorder = $($(popup).children('.arrow-border'));
		if (arrow.length > 0) {
			arrow.css('position', 'absolute');
			arrow.css('z-index', '120');
			arrowBorder.css('position', 'absolute');
			arrowBorder.css('z-index', '119');
		}
		
		$(popup).css('display', 'none');
		$(popup).css('z-index', '121');
		$(popup).css('position', 'absolute');
		
		$(trigger).bind(eventType, function (event) {
			event.stopPropagation();
			event.preventDefault();
			
			if ($(popup).css('display') == 'none') {
				if (beforeOpen != null) beforeOpen.call(popup);
				
				var triggerPos = $(trigger).position();	// relative to the parent
				var trigerBottom = triggerPos.top + $(trigger).height();
				
				var left = horizontalOffset;
				var top = trigerBottom + verticalOffset;;
				if (align == 'center')
					left += triggerPos.left + ($(trigger).outerWidth() - $(popup).outerWidth())/2;
				else if (align == 'right')
					left += triggerPos.left + $(trigger).outerWidth() - $(popup).outerWidth();
				else
					left += triggerPos.left;
	
				$(popup).css('left', left);
				$(popup).css('top', top);
				
				// set the arrows
				if (arrow.length > 0) {
					arrow.css('left', ($(popup).outerWidth()/2 - arrow.outerWidth()/2) + 'px');
					arrow.css('top', '-' + (arrow.outerHeight() - 1) + 'px');
					arrowBorder.css('left', ($(popup).outerWidth()/2 - arrowBorder.outerWidth()/2) + 'px');
					arrowBorder.css('top', '-' + arrowBorder.outerHeight() + 'px');
				}
			} else if (beforeClose != null)
				beforeClose.call(popup);
			$(popup).fadeToggle(fadeDuration);
			return false;
		});

		$(document).click(function (event) {
			if (event.target != $(popup) && event.target != $(trigger) && $(popup).css('display') != 'none' && $.inArray(event.target, $(popup).find('*')) < 0) {
				if (beforeClose != null) beforeClose.call(popup);
				
				$(popup).fadeToggle(fadeDuration);
			}
		});
	};
})(jQuery);