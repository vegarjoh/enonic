if (!lpt) {
    var lpt = {};
}

lpt.AutomaticUpdateController = function () {
    var started = false;

    var completedPortalRequestsTableController;

    var currentPageRequestsController;

    var longestPageRequestsController;

    var longestAttachmentRequestsController;

    var longestImageRequestsController;

    var systemInfoController;

    this.setCompletedPortalRequestsTableController = function (controller) {
        completedPortalRequestsTableController = controller;
    };

    this.setCurrentPageRequestsController = function (controller) {
        currentPageRequestsController = controller;
    };

    this.setLongestPageRequestsController = function (controller) {
        longestPageRequestsController = controller;
    };

    this.setLongestAttachmentRequestsController = function (controller) {
        longestAttachmentRequestsController = controller;
    };

    this.setLongestImageRequestsController = function (controller) {
        longestImageRequestsController = controller;
    };

    this.setSystemInfoController = function (controller) {
        systemInfoController = controller;
    };

    this.switchAutomaticUpdate = function () {
        var autoUpdateCheckbox = document.getElementById("auto-update");
        if (autoUpdateCheckbox.checked == true) {
            this.startAutomaticUpdate();
        }
        else {
            this.stopAutomaticUpdate();
        }
    };

    this.stopAutomaticUpdate = function () {
        if (!started) {
            throw "Illegal state: Trying to stop automatic update when it is already stopped";
        }

        currentPageRequestsController.stopAutomaticReload();
        longestPageRequestsController.stopAutomaticReload();
        longestAttachmentRequestsController.stopAutomaticReload();
        longestImageRequestsController.stopAutomaticReload();
        completedPortalRequestsTableController.stopAutomaticRefresh();
        systemInfoController.stopAutomaticUpdate();

        document.getElementById("fetch-recent-history").disabled = false;

        started = false;
    };

    this.startAutomaticUpdate = function () {
        if (started) {
            throw "Illegal state: Trying to start automatic update when it is already started";
        }

        currentPageRequestsController.startAutomaticReload();
        longestPageRequestsController.startAutomaticReload();
        longestAttachmentRequestsController.startAutomaticReload();
        longestImageRequestsController.startAutomaticReload();
        completedPortalRequestsTableController.startAutomaticRefresh();
        systemInfoController.startAutomaticUpdate();

        document.getElementById("fetch-recent-history").disabled = true;

        started = true;
    }
};
