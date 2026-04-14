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
    const voiceSelect = document.getElementById('voice-select');
    
    // Toggle Layout
    const btnToggleView = document.getElementById('btn-toggle-view');
    let isSinglePageMode = false;

    // -- 0. PDF.js Engine Boot --
    let pdfDoc = null;
    if (window['pdfjs-dist/build/pdf']) {
        window.pdfjsLib = window['pdfjs-dist/build/pdf'];
    }
    if (window.pdfjsLib) {
        window.pdfjsLib.GlobalWorkerOptions.workerSrc = 'js/pdf.worker.min.js';
        
        let loadingTask;
        if (typeof window.SCORM_PDF_B64 !== "undefined" && window.SCORM_PDF_B64) {
            // Conversión de Base64 a Uint8Array explícita
            const binaryString = atob(window.SCORM_PDF_B64);
            const bytes = new Uint8Array(binaryString.length);
            for (let i = 0; i < binaryString.length; i++) {
                bytes[i] = binaryString.charCodeAt(i);
            }
            console.log("Cargando PDF vía buffer Base64 Bypass CORS...");
            loadingTask = window.pdfjsLib.getDocument({data: bytes});
        } else {
            console.log("Cargando PDF vía HTTP URL directo...");
            loadingTask = window.pdfjsLib.getDocument('assets/document.pdf');
        }

        loadingTask.promise.then(doc => {
            pdfDoc = doc;
            console.log("🟢 PDF.js ha cargado el documento vectorizado con éxito. Páginas: " + pdfDoc.numPages);
            updateView(); // Rendereo inicial
        }).catch(err => {
            console.error("🔴 Error PDF.js cargando el documento original: ", err);
        });
    }

    function renderCanvasPage(pageNum, canvasId) {
        if (!pdfDoc) return;
        pdfDoc.getPage(pageNum).then(page => {
            const canvas = document.getElementById(canvasId);
            if (!canvas) return;
            const context = canvas.getContext('2d');
            
            // 1.5 es un buen multiplicador base para HD, luego CSS y zoom del contenedor lo achica
            const viewport = page.getViewport({ scale: 1.5 }); 
            
            canvas.height = viewport.height;
            canvas.width = viewport.width;
            
            const renderContext = { canvasContext: context, viewport: viewport };
            page.render(renderContext);
        });
    }

    // -- 1. Visor Dual / Single --
    function updateView() {
        if (!pdfDoc && window.pdfjsLib) return; // Evitar disparo fantasma si aún carga el PDF.js
        
        if (isSinglePageMode) {
            renderCanvasPage(currentPage, 'page-left-canvas');
            pageLeftContainer.classList.remove('hidden');
            pageRightContainer.classList.add('hidden');
            indicator.textContent = `Página ${currentPage} de ${totalPages}`;
            
            btnPrev.disabled = currentPage <= 1;
            btnNext.disabled = currentPage >= totalPages; 
            
            if (currentPage >= totalPages && scormAPI) {
                scormAPI.LMSSetValue("cmi.core.lesson_status", "completed");
                scormAPI.LMSCommit("");
                console.log("✅ Reporte al LMS SCORM enviado: Completed.");
            }
        } else {
            const showLeft = currentPage <= totalPages;
            const showRight = currentPage + 1 <= totalPages;

            if (showLeft) {
                renderCanvasPage(currentPage, 'page-left-canvas');
                pageLeftContainer.classList.remove('hidden');
            } else {
                pageLeftContainer.classList.add('hidden');
            }

            if (showRight) {
                renderCanvasPage(currentPage + 1, 'page-right-canvas');
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
            btnNext.disabled = currentPage + 1 > totalPages; 
            
            if (currentPage + 1 >= totalPages && scormAPI) {
                scormAPI.LMSSetValue("cmi.core.lesson_status", "completed");
                scormAPI.LMSCommit("");
                console.log("✅ Reporte al LMS SCORM enviado: Completed.");
            }
        }
        
        // Disparador de SCORM Completed
        if (currentPage + 1 >= totalPages && scormAPI) {
            scormAPI.LMSSetValue("cmi.core.lesson_status", "completed");
            scormAPI.LMSCommit("");
            console.log("✅ Reporte al LMS SCORM enviado: Completed.");
        }
        
        // Cortar audio si se cambia de página violentamente
        if (isPlayingAudio) stopAudio();
    }

    // -- 2. Paginación, Zoom y Layout --
    window.goToPage = function(pageNum) {
        currentPage = pageNum;
        if (!isSinglePageMode && currentPage % 2 === 0) {
            currentPage--;
        }
        if (currentPage < 1) currentPage = 1;
        updateView();
        sidebar.classList.add('hidden'); 
    };

    btnPrev.addEventListener('click', () => {
        const step = isSinglePageMode ? 1 : 2;
        if (currentPage > 1) {
            currentPage -= step;
            if (currentPage < 1) currentPage = 1;
            updateView();
        }
    });

    btnNext.addEventListener('click', () => {
        const step = isSinglePageMode ? 1 : 2;
        if (currentPage + step <= totalPages || (!isSinglePageMode && currentPage + 1 <= totalPages)) {
            currentPage += step;
            updateView();
        }
    });
    
    btnToggleView.addEventListener('click', () => {
        isSinglePageMode = !isSinglePageMode;
        btnToggleView.innerHTML = isSinglePageMode ? "📖 2 Pág" : "📖 1 Pág";
        btnToggleView.title = isSinglePageMode ? "Cambiar a 2 páginas" : "Cambiar a 1 página";
        
        // Si volvemos a doble-página, aseguramos que la página actual sea impar (vista de libro)
        if (!isSinglePageMode && currentPage % 2 === 0) {
            currentPage--;
        }
        updateView();
    });

    function applyZoom() {
        // En lugar de usar 'transform: scale()', que genera solapamiento porque los objetos
        // crecen desde sus propios centros hacia afuera sin que flexbox se entere,
        // modificamos la altura física real de la caja contenedora, así flexbox empuja la otra imagen.
        const newHeight = (currentZoom * 90) + 'vh';
        pageLeftContainer.style.height = newHeight;
        pageRightContainer.style.height = newHeight;
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
    let availableVoices = [];

    function populateVoiceList() {
        if (typeof speechSynthesis === 'undefined') return;

        // Limpiar lista actual y buscar las voces en español
        availableVoices = speechSynthesis.getVoices().filter(voice => voice.lang.startsWith('es'));
        
        voiceSelect.innerHTML = '';
        if (availableVoices.length === 0) {
            const option = document.createElement('option');
            option.textContent = 'Voz predeterminada del sistema';
            option.value = '';
            voiceSelect.appendChild(option);
            return;
        }

        const savedVoiceURI = localStorage.getItem('scorm_preferred_voice');
        
        availableVoices.forEach((voice) => {
            const option = document.createElement('option');
            option.textContent = voice.name;
            option.value = voice.voiceURI;
            
            // Seleccionar guardada
            if (savedVoiceURI && voice.voiceURI === savedVoiceURI) {
                option.selected = true;
            }
            voiceSelect.appendChild(option);
        });
        
        // Auto-seleccionar una buena voz o la principal si no hay elección guardada
        if (!savedVoiceURI && availableVoices.length > 0) {
            const bestVoiceObj = availableVoices.find(v => v.name.includes('Neural') || v.name.includes('Google'));
            if (bestVoiceObj) {
                voiceSelect.value = bestVoiceObj.voiceURI;
            } else {
                voiceSelect.value = availableVoices[0].voiceURI;
            }
        }
    }

    if (speechSynthesis !== undefined) {
        populateVoiceList();
        if (speechSynthesis.onvoiceschanged !== undefined) {
            speechSynthesis.onvoiceschanged = populateVoiceList;
        }
    }

    voiceSelect.addEventListener('change', () => {
        localStorage.setItem('scorm_preferred_voice', voiceSelect.value);
        if (isPlayingAudio) {
            stopAudio();
            playAudio();
        }
    });

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
            
            if (!isSinglePageMode && currentPage + 1 <= totalPages && window.SCORM_PAGES_TEXT && window.SCORM_PAGES_TEXT[currentPage + 1]) {
                textToRead += " ... " + window.SCORM_PAGES_TEXT[currentPage + 1]; 
            }

            if (textToRead.trim().length === 0) {
                alert("No detecté texto vivo en estas páginas (tal vez son fotos completas).");
                return;
            }

            const utterance = new SpeechSynthesisUtterance(textToRead);
            utterance.lang = "es-ES";

            // Asignar voz elegida del combo
            const selectedURI = voiceSelect.value;
            if (selectedURI) {
                const selectedVoice = availableVoices.find(v => v.voiceURI === selectedURI);
                if (selectedVoice) {
                    utterance.voice = selectedVoice;
                }
            }
            
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
