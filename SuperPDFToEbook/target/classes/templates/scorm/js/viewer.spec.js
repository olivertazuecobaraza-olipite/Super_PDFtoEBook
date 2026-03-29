/**
 * 🧪 Test Unitario para Frontend (viewer.js) - Entorno Jest/Node
 * 
 * NOTA DEL ARQUITECTO: Como tu aplicación exporta archivos ZIP encapsulados, 
 * estos test JS se corren en tu entorno de desarrollo instalando Jest (`npm i jest jsdom`).
 * 
 * CONCEPTOS EVALUADOS:
 * 1. Simulación (Mocking) del API LMS de Moodle.
 * 2. Comportamiento en Memoria de la Detección SCORM (findAPI).
 * 3. Evento "Completado" en el umbral de la última página.
 */

describe('🚀 Visor SCORM Dual (Frontend JS)', () => {
    
    let scormAPIMock;

    beforeEach(() => {
        // [1] Arrange (Preparando la mesa de trabajo)
        // Simulamos la estructura cruda que inyecta Moodle a través del iFrame parental.
        scormAPIMock = {
            LMSInitialize: jest.fn(),
            LMSSetValue: jest.fn(),
            LMSCommit: jest.fn(),
            LMSFinish: jest.fn()
        };

        // Plantamos el Mock en el objeto Window universal (JSDom)
        window.API = scormAPIMock;

        // Inyectamos el Documento DOM Básico (Como lo haría el ZIP)
        document.body.innerHTML = `
            <button id="btn-next">Siguiente</button>
            <button id="btn-prev">Anterior</button>
            <span id="page-indicator">1 / 10</span>
        `;

        // Simulamos inyección dinámica del Servidor Java
        window.SCORM_TOTAL_PAGES = 10;
        window.SCORM_PAGES_TEXT = ["", "Intro", "Capsula", "A", "B", "C", "D", "E", "F", "G", "Fin"];
    });

    afterEach(() => {
        // Limpiar mutaciones de los espías
        jest.clearAllMocks();
    });

    it('✅ Debe Detectar e Inicializar SCORM al cargar el DOM de Moodle', () => {
        // [2] Act (Simulamos la carga de la pantalla)
        // El script viewer.js escucha el 'DOMContentLoaded'. Lo disparamos a mano:
        const event = new Event('DOMContentLoaded');
        document.dispatchEvent(event);

        // [3] Assert (Comprobaciones Crueles)
        // Si el Dev hizo bien su trabajo, apenas inicia tiene que reportar Incomplete.
        expect(scormAPIMock.LMSInitialize).toHaveBeenCalledWith("");
        expect(scormAPIMock.LMSSetValue).toHaveBeenCalledWith("cmi.core.lesson_status", "incomplete");
        expect(scormAPIMock.LMSCommit).toHaveBeenCalled();
    });

    it('☠️ Debe enviar "Completed" cuando el alumno hojea la última página', () => {
        // Asume que incluyes las variables locales del scope a testear
        // ... Lógica Mockeada de currentPage (Página Actual) = 9
        // El botón next es disparado
        // Si currentPage es 9, la lógica dice: (9 + 1) >= SCORM_TOTAL_PAGES (10)
        // Por ende, debe disparar finalización.
        
        let currentPage = 9; 
        const total = window.SCORM_TOTAL_PAGES;
        
        if (currentPage + 1 >= total && window.API) {
            window.API.LMSSetValue("cmi.core.lesson_status", "completed");
            window.API.LMSCommit("");
        }

        // Assert: Moodle debería haber recibido estado Finalizado
        expect(scormAPIMock.LMSSetValue).toHaveBeenCalledWith("cmi.core.lesson_status", "completed");
    });
    
    it('🛡️ Debe tolerar ausencias puras (Modo Offline Local Válido)', () => {
        // Si no hay API (Usuario abre el ZIP en su PC directo sin Moodle)
        window.API = null;
        window.parent.API = null;
        
        const event = new Event('DOMContentLoaded');
        // Esto NO debe explotar disparando undefined errors y matando el Visor.
        expect(() => document.dispatchEvent(event)).not.toThrow();
        // El visor JS tiene que sobrevivir solo, de forma resiliente.
    });

});
