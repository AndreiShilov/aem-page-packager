/* globals Granite*/
(function ($) {

    $(document).on('click', '.js-page-packager', function () {
        window.open(this.dataset.path, '_blank');
    });

})(Granite.$);
