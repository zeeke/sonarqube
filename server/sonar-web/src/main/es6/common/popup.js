define(['backbone.marionette'], function (Marionette) {

  var $ = jQuery;


  class PopupView extends Marionette.ItemView {
    onRender() {
      var that = this,
          body = $('body'),
          trigger = this.options.triggerEl,
          offset = trigger.offset();

      this.$el.detach().appendTo(body);

      if (this.options.bottom) {
        this.$el.addClass('bubble-popup-bottom').css({
          top: offset.top + trigger.outerHeight(),
          left: offset.left
        });
      } else if (this.options.bottomRight) {
        this.$el.addClass('bubble-popup-bottom-right').css({
          top: offset.top + trigger.outerHeight(),
          right: $(window).width() - offset.left - trigger.outerWidth()
        });
      } else {
        this.$el.css({
          top: offset.top,
          left: offset.left + trigger.outerWidth()
        });
      }

      body.on('click.bubble-popup', () => {
        body.off('click.bubble-popup');
        that.close();
      });
    }
  }

  PopupView.prototype.className = 'bubble-popup';

  return PopupView;
});
