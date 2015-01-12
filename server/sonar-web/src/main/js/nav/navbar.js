define([
  'templates/nav'
], function () {

  var $ = jQuery;

  return Marionette.Layout.extend({
    className: 'navbar',
    tagName: 'nav',
    template: Templates['nav-navbar'],

    initialize: function () {
      $(window).on('scroll.nav-layout', this.onScroll);
      this.projectName = window.navbarProject;
    },

    onClose: function () {
      $(window).off('scroll.nav-layout');
    },

    onScroll: function () {
      var scrollTop = $(window).scrollTop(),
          isInTheMiddle = scrollTop > 0;
      $('.navbar-sticky').toggleClass('middle', isInTheMiddle);
    },

    onRender: function () {
      this.$el.toggleClass('navbar-primary', !!this.projectName);
    },

    serializeData: function () {
      console.log(this.projectName);
      return _.extend(Marionette.Layout.prototype.serializeData.apply(this, arguments), {
        user: window.SS.user,
        userName: window.SS.userName,
        isUserAdmin: window.SS.isUserAdmin,

        projectName: this.projectName,
        projectFavorite: window.navbarProjectFavorite,
        navbarCanFavoriteProject: window.navbarCanFavoriteProject
      });
    }
  });

});
