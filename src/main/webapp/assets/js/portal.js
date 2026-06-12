(function () {
    var portal = document.getElementById("portalScroll");
    if (!portal) {
        return;
    }

    var body = document.body;
    var sections = Array.prototype.slice.call(document.querySelectorAll("[data-portal-section]"));
    var jumps = Array.prototype.slice.call(document.querySelectorAll("[data-portal-jump]"));
    var cue = document.querySelector(".portal-scroll-cue");
    var currentIndex = 0;
    var wheelLocked = false;
    var touchStartY = 0;
    var lockTimer = null;

    function resetNativeScroll() {
        if (window.pageXOffset !== 0 || window.pageYOffset !== 0) {
            window.scrollTo(0, 0);
        }
    }

    function normalizeIndex(index) {
        return Math.max(0, Math.min(sections.length - 1, index));
    }

    function findIndexById(id) {
        return sections.findIndex(function (section) {
            return section.id === id;
        });
    }

    function setActive(index, updateHash) {
        currentIndex = normalizeIndex(index);
        var activeSection = sections[currentIndex];
        if (!activeSection) {
            return;
        }

        resetNativeScroll();
        body.setAttribute("data-portal-index", String(currentIndex));
        sections.forEach(function (section, sectionIndex) {
            section.classList.toggle("active", sectionIndex === currentIndex);
        });
        jumps.forEach(function (item) {
            item.classList.toggle("active", item.getAttribute("data-portal-jump") === activeSection.id);
        });
        if (cue) {
            cue.classList.toggle("hidden", currentIndex === sections.length - 1);
        }
        if (updateHash && history.replaceState) {
            history.replaceState(null, "", "#" + activeSection.id);
        }
    }

    function lockWheel() {
        wheelLocked = true;
        window.clearTimeout(lockTimer);
        lockTimer = window.setTimeout(function () {
            wheelLocked = false;
        }, 820);
    }

    function goToIndex(index, updateHash) {
        var nextIndex = normalizeIndex(index);
        if (nextIndex === currentIndex) {
            return;
        }
        setActive(nextIndex, updateHash !== false);
        lockWheel();
    }

    function goToId(id) {
        var nextIndex = findIndexById(id);
        if (nextIndex >= 0) {
            goToIndex(nextIndex, true);
        }
    }

    jumps.forEach(function (item) {
        item.addEventListener("click", function (event) {
            var id = item.getAttribute("data-portal-jump");
            if (!id || findIndexById(id) < 0) {
                return;
            }
            event.preventDefault();
            goToId(id);
        });
    });

    window.addEventListener("wheel", function (event) {
        if (Math.abs(event.deltaY) < 8) {
            return;
        }
        event.preventDefault();
        if (wheelLocked) {
            return;
        }
        goToIndex(currentIndex + (event.deltaY > 0 ? 1 : -1), true);
    }, { passive: false });

    document.addEventListener("keydown", function (event) {
        if (event.defaultPrevented || /input|textarea|select/i.test(event.target.tagName)) {
            return;
        }
        if (event.key === "ArrowDown" || event.key === "PageDown" || event.key === " ") {
            event.preventDefault();
            goToIndex(currentIndex + 1, true);
        }
        if (event.key === "ArrowUp" || event.key === "PageUp") {
            event.preventDefault();
            goToIndex(currentIndex - 1, true);
        }
        if (event.key === "Home") {
            event.preventDefault();
            goToIndex(0, true);
        }
        if (event.key === "End") {
            event.preventDefault();
            goToIndex(sections.length - 1, true);
        }
    });

    window.addEventListener("touchstart", function (event) {
        if (!event.touches || !event.touches.length) {
            return;
        }
        touchStartY = event.touches[0].clientY;
    }, { passive: true });

    window.addEventListener("touchend", function (event) {
        if (!event.changedTouches || !event.changedTouches.length || wheelLocked) {
            return;
        }
        var delta = touchStartY - event.changedTouches[0].clientY;
        if (Math.abs(delta) < 44) {
            return;
        }
        goToIndex(currentIndex + (delta > 0 ? 1 : -1), true);
    }, { passive: true });

    if (window.location.hash) {
        var initialIndex = findIndexById(window.location.hash.substring(1));
        if (initialIndex >= 0) {
            setActive(initialIndex, false);
            window.setTimeout(resetNativeScroll, 0);
            return;
        }
    }

    setActive(0, false);
}());
