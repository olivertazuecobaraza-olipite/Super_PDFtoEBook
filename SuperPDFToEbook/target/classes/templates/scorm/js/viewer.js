document.addEventListener('DOMContentLoaded', () => {
    // --- Módulo Conector SCORM 1.2 (Moodle/LMS) ---
    let scormAPI = null;
    function findAPI(win) {
        let findAPITries = 0;
        while((win.API == null) && (win.parent != null) && (win.parent != win)) {
            findAPITries++;
            if (findAPITries > 7) { return null; }
            win = win.parent;
        }
        return win.API;
    }
    
    scormAPI = findAPI(window);
    if ((scormAPI == null) && (window.opener != null) && (typeof(window.opener) != "undefined")) {
        scormAPI = findAPI(window.opener);
    }
    
    if (scormAPI) {
        console.log("🟢 API SCORM Moodle/LMS Detectada con éxito.");
        scormAPI.LMSInitialize("");
        scormAPI.LMSSetValue("cmi.core.lesson_status", "incomplete");
        scormAPI.LMSCommit("");
    } else {
        console.warn("🟡 Trabajando en modo Offline local. Sin API SCORM activa.");
    }
    
    window.addEventListener("unload", () => {
        if (scormAPI) {
            scormAPI.LMSFinish("");
        }
    });
    // --- Fin Conector SCORM ---

    const totalPages = window.SCORM_TOTAL_PAGES || 1;
    let currentPage = 1;
    let currentZoom = 1;
    let isPlayingAudio = false;

    // Elements
    const pageLeft = document.getElementById('page-left');
    const pageRight = document.getElementById('page-right');
    const pageLeftContainer = document.getElementById('page-left-container');
    const pageRightContainer = document.getElementById('page-right-container');
    const btnPrev = document.getElementById('btn-prev');
    const btnNext = document.getElementById('btn-next');
    const indicator = document.getElementById('page-indicator');
    const btnZoomIn = document.getElementById('btn-zoom-in');
    const btnZoomOut = document.getElementById('btn-zoom-out');
    
    // Sidebar & Audio
    const btnOutline = document.getElementById('btn-outline');
    const sidebar = document.getElementById('sidebar');
    const btnCloseSidebar = document.getElementById('btn-close-sidebar');
    const btnAudio = document.getElementById('btn-audio');

    // -- 1. Visor Dual --
    function updateView() {
        const showLeft = currentPage <= totalPages;
        const showRight = currentPage + 1 <= totalPages;

        if (showLeft) {
            pageLeft.src = `assets/pages/${currentPage}.jpg`;
            pageLeftContainer.classList.remove('hidden');
        } else {
            pageLeftContainer.classList.add('hidden');
        }

        if (showRight) {
            pageRight.src = `assets/pages/${currentPage + 1}.jpg`;
            pageRightContainer.classList.remove('hidden');
        } else {
            pageRightContainer.classList.add('hidden');
        }

        if (showRight) {
            indicator.textContent = `Página ${currentPage} - ${currentPage + 1} de ${totalPages}`;
        } else {
            indicator.textContent = `Página ${currentPage} de ${totalPages}`;
        }

        btnPrev.disabled = currentPage <= 1;
        btnNext.disabled = currentPage + 1 >= totalPages; 
        
        // Disparador de SCORM Completed
        if (currentPage + 1 >= totalPages && scormAPI) {
            scormAPI.LMSSetValue("cmi.core.lesson_status", "completed");
            scormAPI.LMSCommit("");
            console.log("✅ Reporte al LMS SCORM enviado: Completed.");
        }
        
        // Cortar audio si se cambia de página violentamente
        if (isPlayingAudio) stopAudio();
    }

    // -- 2. Paginación y Zoom --
    window.goToPage = function(pageNum) {
        // Asegurarse de que arranca en impar para mantener vista de libro
        currentPage = (pageNum % 2 === 0) ? pageNum - 1 : pageNum;
        if (currentPage < 1) currentPage = 1;
        updateView();
        sidebar.classList.add('hidden'); // Ocultar sidebar al salta
    };

    btnPrev.addEventListener('click', () => {
        if (currentPage > 1) {
            currentPage -= 2;
            if (currentPage < 1) currentPage = 1;
            updateView();
        }
    });

    btnNext.addEventListener('click', () => {
        if (currentPage + 1 < totalPages) {
            currentPage += 2;
            updateView();
        }
    });

    function applyZoom() {
        pageLeftContainer.style.transform = `scale(${currentZoom})`;
        pageRightContainer.style.transform = `scale(${currentZoom})`;
    }

    btnZoomIn.addEventListener('click', () => {
        if (currentZoom < 2.5) { currentZoom += 0.2; applyZoom(); }
    });
    btnZoomOut.addEventListener('click', () => {
        if (currentZoom > 0.6) { currentZoom -= 0.2; applyZoom(); }
    });

    window.addEventListener('keydown', (e) => {
        if (e.key === 'ArrowRight') btnNext.click();
        if (e.key === 'ArrowLeft') btnPrev.click();
    });

    // -- 3. Sidebar Outline (Índice) --
    btnOutline.addEventListener('click', () => {
        sidebar.classList.toggle('hidden');
    });
    btnCloseSidebar.addEventListener('click', () => {
        sidebar.classList.add('hidden');
    });

    // -- 4. Modulo TTS Audio (Audilibro vía APIs de Navegador) --
    function stopAudio() {
        speechSynthesis.cancel();
        isPlayingAudio = false;
        btnAudio.innerHTML = "▶ Audio";
        btnAudio.style.background = "rgba(255, 255, 255, 0.1)";
    }

    async function playAudio() {
        if (isPlayingAudio) {
            stopAudio();
            return;
        }

        try {
            // El usuario ve 2 páginas, levantamos el texto inyectado Offline por el Generador
            let textToRead = "";
            
            // Array global asegurado, el índice 0 es basura, páginas base-1
            if (window.SCORM_PAGES_TEXT && window.SCORM_PAGES_TEXT[currentPage]) {
                textToRead += window.SCORM_PAGES_TEXT[currentPage];
            }
            
            if (currentPage + 1 <= totalPages && window.SCORM_PAGES_TEXT && window.SCORM_PAGES_TEXT[currentPage + 1]) {
                textToRead += " ... " + window.SCORM_PAGES_TEXT[currentPage + 1]; 
            }

            if (textToRead.trim().length === 0) {
                alert("No detecté texto vivo en estas páginas (tal vez son fotos completas).");
                return;
            }

            const utterance = new SpeechSynthesisUtterance(textToRead);
            utterance.lang = "es-ES";
            utterance.onend = () => stopAudio();

            speechSynthesis.speak(utterance);
            isPlayingAudio = true;
            btnAudio.innerHTML = "⏸ Stop Audio";
            btnAudio.style.background = "var(--primary-color)";

        } catch (error) {
            console.error("Error reproduciendo audio TTS:", error);
        }
    }

    btnAudio.addEventListener('click', playAudio);

    // Initial Trigger
    updateView();
});
