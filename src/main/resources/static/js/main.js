document.addEventListener('DOMContentLoaded', function() {
    const ricMatrix = window.ricMatrix || [];
    // Deleting old rows from the FD table
    const fdTbody = document.querySelector('#fdTable tbody');
    if (fdTbody) fdTbody.innerHTML = '';

    // Adding and deleting functional dependencies
    const addFdButton = document.getElementById('addFdBtn');
    const fdTableBody = document.getElementById('fdTable')?.querySelector('tbody');
    const clearManualDataBtn = document.getElementById('clearManualDataBtn');
    const clearFdRowsBtn = document.getElementById('clearFdRowsBtn');


     // Builds a FD table row so numbering, arrow cell and delete button
    const buildFdRowHtml = (rowNumber, left = '', right = '') => `
                <td>${rowNumber}</td>
                <td contenteditable>${left}</td>
                <td>→</td>
                <td contenteditable>${right}</td>
                <td><button type="button" class="delFd">×</button></td>
            `;

    // Appends a fresh FD row to the table body and auto-increments row numbers.
    const appendFdRow = (left = '', right = '') => {
        if (!fdTableBody) return;
        const tr = document.createElement('tr');
        tr.innerHTML = buildFdRowHtml(fdTableBody.rows.length + 1, left, right);
        fdTableBody.appendChild(tr);
    };

    // Clears the FD table and leaves a single empty row.
    const resetFdTable = () => {
        if (!fdTableBody) return;
        fdTableBody.innerHTML = '';
        appendFdRow();
    };

    if (addFdButton && fdTableBody) {
        addFdButton.addEventListener('click', () => {
            appendFdRow();
        });
        fdTableBody.addEventListener('click', e => {
            if (e.target.matches('.delFd')) {
                e.target.closest('tr').remove();
                Array.from(fdTableBody.rows).forEach((r, i) => r.cells[0].textContent = i + 1);
                if (fdTableBody.rows.length === 0) {
                    appendFdRow();
                }
            }
        });

        // If the table is empty (no restored data), insert a blank row
        if (fdTableBody.rows.length === 0) {
            appendFdRow();
        }
    }

    // Dynamic manual data section
    const manualDataTable = document.getElementById('manualDataTable');
    const addRowButton = document.getElementById('addRow');
    const updateColumnsBtn = document.getElementById('updateColumnsBtn');
    const columnCountInput = document.getElementById('columnCountInput');

    // Main function that updates the table according to the desired number of columns
    function updateManualTable(colCount) {
        if (!manualDataTable || colCount < 1) return;

        const thead = manualDataTable.querySelector('thead');
        const tbody = manualDataTable.querySelector('tbody');

        // Clear existing header and body
        thead.innerHTML = '';
        tbody.innerHTML = '';

        // Create new header
        const headerRow = thead.insertRow();
        for (let i = 1; i <= colCount; i++) {
            const th = document.createElement('th');
            th.textContent = i;
            headerRow.appendChild(th);
        }
        // Add a cell titled "Del" for the delete button
        const delTh = document.createElement('th');
        delTh.textContent = 'Del';
        headerRow.appendChild(delTh);

        // Add at least one blank line
        addManualRow();
    }

    // Function that adds a new row
    function addManualRow() {
        const tbody = manualDataTable.querySelector('tbody');
        if (!tbody) return;

        // Read the number of columns directly from input
        const colCount = parseInt(columnCountInput.value, 10);
        if (colCount < 1) return;

        const newRow = tbody.insertRow();

        // Add editable cells (td) as many times as the number of columns
        for (let i = 0; i < colCount; i++) {
            const cell = newRow.insertCell();
            cell.contentEditable = true;
        }

        // Add row delete button
        const deleteCell = newRow.insertCell();
        deleteCell.innerHTML = `<button type="button" class="delManualRow">×</button>`;
    }

     // Removes all manual data rows, inserts one blank row
    function clearManualDataTable() {
        if (!manualDataTable) return;
        const tbody = manualDataTable.querySelector('tbody');
        if (!tbody) return;
        tbody.innerHTML = '';
        addManualRow();
        const manualDataInput = document.getElementById('manualData');
        if (manualDataInput) manualDataInput.value = '';
        syncManualDataFromTable();
    }

     // Clears the FD grid
    function clearFunctionalDependencies() {
        resetFdTable();
        const fdInput = document.getElementById('fdsInput');
        if (fdInput) fdInput.value = '';
    }

    // Function to detect and remove duplicate tuples from manual data table
    function detectAndRemoveDuplicateTuples() {
        const tbody = manualDataTable.querySelector('tbody');
        if (!tbody) return 0;

        const rows = Array.from(tbody.querySelectorAll('tr'));
        const seenTuples = new Set();
        const rowsToRemove = [];

        rows.forEach(row => {
            const cells = Array.from(row.querySelectorAll('td[contenteditable]'));
            const tupleContent = cells.map(cell => cell.textContent.trim()).join(',');

            // Skip completely empty rows
            if (tupleContent.replace(/,/g, '').trim() === '') {
                return;
            }

            // Normalize the tuple (remove extra spaces, standardize format)
            const normalizedTuple = tupleContent.toLowerCase().replace(/\s+/g, '');

            if (seenTuples.has(normalizedTuple)) {
                // This is a duplicate
                rowsToRemove.push(row);
            } else {
                seenTuples.add(normalizedTuple);
            }
        });

        // Remove duplicate rows
        rowsToRemove.forEach(row => row.remove());

        return rowsToRemove.length;
    }

    if (clearManualDataBtn) {
        clearManualDataBtn.addEventListener('click', clearManualDataTable);
    }

    if (clearFdRowsBtn) {
        clearFdRowsBtn.addEventListener('click', clearFunctionalDependencies);
    }

    // Update the table when the "Update Table" button is clicked
    if (updateColumnsBtn && columnCountInput) {
        updateColumnsBtn.addEventListener('click', () => {
            const count = parseInt(columnCountInput.value, 10);
            updateManualTable(count);
        });
    }

    // Add a new row when the "+ Add Row" button is clicked
    if (addRowButton) {
        addRowButton.addEventListener('click', addManualRow);
    }

    // Manage row deletion event (with event deletion)
    if (manualDataTable) {
        manualDataTable.addEventListener('click', e => {
            if (!e.target.matches('.delManualRow')) {
                return;
            }

            const row = e.target.closest('tr');
            if (!row) return;

            const tbody = manualDataTable.querySelector('tbody');
            if (!tbody) return;

            const rows = Array.from(tbody.rows);
            const isOnlyRow = rows.length === 1 && rows[0] === row;
            const rowIsEmpty = Array.from(row.querySelectorAll('td[contenteditable]'))
                .every(cell => cell.textContent.trim() === '');

            // Keep a single placeholder row if it is empty
            if (isOnlyRow && rowIsEmpty) {
                return;
            }

            row.remove();

            // Ensure table never stays empty
            if (tbody.rows.length === 0) {
                addManualRow();
            }
        });

        // Initialize table with default 3 columns on page load
        updateManualTable(3);
    }

    // Preview CSV files
    const csvInput = document.getElementById('csvfile');
    let fullCsvData = [];
    if (csvInput) {
        csvInput.addEventListener('change', () => {
            const file = csvInput.files[0];
            if (!file) return;
            const reader = new FileReader();
            reader.onload = evt => {
                const text = evt.target.result;
                // Use delimiter auto-detection to support CSV files
                const parsed = Papa.parse(text, {
                    skipEmptyLines: true,
                    delimiter: "",  // Empty string triggers auto-detection
                    delimitersToGuess: [',', '\t', ';', '|']  // Common delimiters to check
                });
                if (parsed.errors?.length) {
                    console.error('CSV parse errors', parsed.errors);
                    Swal.fire({
                        icon: 'error',
                        title: 'CSV could not read',
                        text: parsed.errors[0].message,
                        confirmButtonText: 'Close'
                    });
                    return;
                }
                const rows = parsed.data;
                fullCsvData = rows;
                populateManualTableFromParsedCsv(rows);
                const duplicateCount = detectAndRemoveDuplicateTuples();
                syncManualDataFromTable();
                console.info('Duplicate rows removed:', duplicateCount);
            };
            reader.readAsText(file);
        });
    }

    function populateManualTableFromParsedCsv(rows) {
        if (!Array.isArray(rows) || rows.length === 0) return;
        const columnCount = rows[0].length;
        const columnCountInput = document.getElementById('columnCountInput');
        if (columnCountInput) columnCountInput.value = columnCount;
        updateManualTable(columnCount);
        const tbody = manualDataTable.querySelector('tbody');
        if (!tbody) return;
        tbody.innerHTML = '';
        rows.forEach(rowValues => {
            const newRow = tbody.insertRow();
            for (let i = 0; i < columnCount; i++) {
                const newCell = newRow.insertCell();
                newCell.setAttribute('contenteditable', 'true');
                newCell.textContent = rowValues[i] ?? '';
            }
            const deleteCell = newRow.insertCell();
            deleteCell.innerHTML = '<button type="button" class="delManualRow">×</button>';
        });
    }

    function syncManualDataFromTable() {
        const manualRows = Array.from(document.querySelectorAll('#manualDataTable tbody tr'));
        // Serialize rows - quote values that contain comma or semicolon
        const rowsData = manualRows.map(row => {
            const cells = Array.from(row.querySelectorAll('td[contenteditable]'));
            return cells.map(cell => cell.textContent.trim());
        }).filter(row => row.some(cell => cell !== ''));

        const serializedRows = rowsData.map(row => {
            const quotedCells = row.map(cell => {
                // If cell contains comma, semicolon, or double quote, wrap in quotes and escape internal quotes
                if (cell.includes(',') || cell.includes(';') || cell.includes('"')) {
                    const escaped = cell.replace(/"/g, '""');
                    return `"${escaped}"`;
                }
                return cell;
            });
            return quotedCells.join(',');
        });
        const serialized = serializedRows.join(';');
        document.getElementById('manualData').value = serialized;
    }

    // Take CSV rows and fill manual FD table
    function populateFdTableFromCsv(fdLines) {
        const fdTableBody = document.querySelector('#fdTable tbody');
        if (!fdTableBody) return;
        // Clear existing FDs
        fdTableBody.innerHTML = '';
        fdLines.forEach((fdText, index) => {
            // Normalize the separator
            const line = fdText.replace(/→/g, '->');
            const parts = line.split('->');
            if (parts.length !== 2) return;

            const left = parts[0].trim();
            const right = parts[1].trim();

            const newRow = fdTableBody.insertRow();
            newRow.innerHTML = buildFdRowHtml(index + 1, left, right);
        });

        // Rearrange line numbers
        Array.from(fdTableBody.rows).forEach((r, i) => r.cells[0].textContent = i + 1);
    }

    // Functional dependencies file preview section
    const fdFileInput = document.getElementById('fdfile');
    let fullFdData     = [];
    if (fdFileInput) {
        fdFileInput.addEventListener('change', () => {
            const file = fdFileInput.files[0];
            if (!file) return;
            const reader = new FileReader();
            reader.onload = evt => {
                // Read, normalize and filter FD rows
                fullFdData = evt.target.result
                    .split(/\r?\n/)
                    // Normalize the separator
                    .map(l => l.trim().replace(/→/g, '->'))
                    // Keep only valid FD rows
                    .filter(l => l !== '' && l.includes('->'));

                // Fill manual FD table
                populateFdTableFromCsv(fullFdData);
            };
            reader.readAsText(file);
        });
    }

    // Adding Monte Carlo optimization checkbox
    const monteCarloCheckbox = document.getElementById('mcCheckbox');
    const samples = document.getElementById('samples');
    if (monteCarloCheckbox && samples) {
        const params = new URLSearchParams(window.location.search);
        const restoredMonteCarlo = params.get('monteCarlo');
        const restoredSamples = params.get('samples');
        if (restoredMonteCarlo !== null) {
            const shouldCheck = restoredMonteCarlo === 'true' || restoredMonteCarlo === 'on' || restoredMonteCarlo === '1';
            monteCarloCheckbox.checked = shouldCheck;
            samples.disabled = !shouldCheck;
        }
        if (restoredSamples !== null && restoredSamples !== '') {
            samples.value = restoredSamples;
        } else {
            samples.value = '100000';
        }
        monteCarloCheckbox.addEventListener('change', () => {
            samples.disabled = !monteCarloCheckbox.checked;
        });
    }

    // Collecting data with using form submit
    const form = document.getElementById('calcForm');
    if (!form) return console.error("Form could not be found.");

    const computeBtn = document.getElementById('computeBtn');

    form.addEventListener('submit', e => {
        // Stop form submit by default
        e.preventDefault();

        // Detect and remove duplicate tuples before processing
        const duplicateCount = detectAndRemoveDuplicateTuples();

        // Collecting CSV file or manual data
        let manualContent = '';

        // Always read from the table as the manual table is the only and editable data source
        const manualRows = Array.from(document.querySelectorAll('#manualDataTable tbody tr'));
        // Use PapaParse to properly serialize rows with values containing commas or semicolons
        const rowsData = manualRows.map(row => {
            const cells = Array.from(row.querySelectorAll('td[contenteditable]'));
            return cells.map(cell => cell.textContent.trim());
        }).filter(row => row.some(cell => cell !== ''));

        // Serialize each row - quote values that contain comma or semicolon
        const serializedRows = rowsData.map((row, rowIndex) => {
            const quotedCells = row.map((cell, cellIndex) => {
                // If cell contains comma, semicolon, or double quote, wrap in quotes and escape internal quotes
                if (cell.includes(',') || cell.includes(';') || cell.includes('"')) {
                    // Escape existing double quotes by doubling them
                    const escaped = cell.replace(/"/g, '""');
                    return `"${escaped}"`;
                }
                return cell;
            });
            return quotedCells.join(',');
        });
        manualContent = serializedRows.join(';');
        document.getElementById('manualData').value = manualContent;

        // Collecting functional dependencies
        let fdLines = Array.from(document.querySelectorAll('#fdTable tbody tr'))
            .map(row => {
                const lhs = row.cells[1].textContent.trim();
                const rhs = row.cells[3].textContent.trim();
                return lhs && rhs ? `${lhs}->${rhs}` : null;
            })
            .filter(s => s);
        document.getElementById('fdsInput').value = fdLines.join(';');


        const finalDataString = manualContent.trim();
        const finalFdsString = fdLines.join(';').trim();

        const isDataMissing = finalDataString.length === 0;
        const isFdsMissing = finalFdsString.length === 0;

        // Both Data and FD are missing
        if (isDataMissing && isFdsMissing) {
            Swal.fire({
                icon: 'error',
                title: 'No input was entered',
                text: 'Please enter data and functional dependencies.',
                confirmButtonText: 'Close'
            });
            return;
        }

        // Only table data is missing
        if (isDataMissing) {
            Swal.fire({
                icon: 'error',
                title: 'Table Data Missing',
                text: 'Please enter any table data (manual or CSV).',
                confirmButtonText: 'Close'
            });
            return;
        }

        const modeInput = document.getElementById('plaqueModeInput');
        const requestMode = (modeInput && modeInput.value) ? modeInput.value : (window.plaqueMode || 'enabled');

        startLiveComputation({
            manualData: finalDataString,
            fds: finalFdsString,
            monteCarloSelected: !!(document.getElementById('mcCheckbox')?.checked),
            samples: document.getElementById('samples')?.value || '100000',
            duplicatesRemoved: duplicateCount,
            mode: requestMode
        });
    });

    function startLiveComputation({ manualData, fds, monteCarloSelected, samples, duplicatesRemoved, mode }) {
        if (computeBtn) computeBtn.disabled = true;

        const normalizedMode = (mode || window.plaqueMode || 'enabled').toString().toLowerCase();
        const isNoPlaque = normalizedMode === 'disabled' || normalizedMode === 'no-plaque';

        // NO-PLAQUE mode: Skip live computation modal, submit form directly
        if (isNoPlaque) {
            console.log('[main.js] NO-PLAQUE mode: Skipping live computation modal, submitting form directly');
            document.getElementById('calcForm').submit();
            return;
        }

        // WITH-PLAQUE mode: Show live computation status modal
        const progressSteps = [];
        let timerInterval = null;
        const streamStartedAt = performance.now();
        let lastProgressAt = performance.now();
        const MIN_PROGRESS_MS = 1200;
        const MIN_AFTER_LAST_PROGRESS_MS = 600;

        // Format elapsed time
        const formatElapsedTime = (ms) => {
            const seconds = Math.floor(ms / 1000);
            const minutes = Math.floor(seconds / 60);
            const remainingSeconds = seconds % 60;
            const milliseconds = Math.floor((ms % 1000) / 10);
            if (minutes > 0) {
                return `${minutes}m ${remainingSeconds.toString().padStart(2, '0')}.${milliseconds.toString().padStart(2, '0')}s`;
            }
            return `${remainingSeconds}.${milliseconds.toString().padStart(2, '0')}s`;
        };

        // Build professional timeline HTML
        // backendElapsedMs: If provided (from complete event), use this for final elapsed time display
        const buildTimelineHtml = (steps, status = 'running', backendElapsedMs = null) => {
            // Use backend elapsed time for completion, frontend time for running status
            const elapsedMs = (status === 'success' && backendElapsedMs !== null)
                ? backendElapsedMs
                : (performance.now() - streamStartedAt);
            const elapsedFormatted = formatElapsedTime(elapsedMs);

            // Build timeline steps
            const stepsHtml = steps.map((step, index) => {
                const isLast = index === steps.length - 1;
                const stepStatus = isLast && status === 'running' ? 'active' : 'completed';
                const dotContent = stepStatus === 'completed'
                    ? '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"></polyline></svg>'
                    : (index + 1);
                const badgeHtml = stepStatus === 'active'
                    ? '<span class="live-status-badge live-status-badge--running">In Progress</span>'
                    : '<span class="live-status-badge live-status-badge--completed">Completed</span>';

                return `
                    <div class="live-status-step ${stepStatus}" role="listitem" aria-current="${stepStatus === 'active' ? 'step' : 'false'}">
                        <div class="live-status-step-dot" aria-hidden="true">${dotContent}</div>
                        <div class="live-status-step-content">
                            <span class="live-status-step-text">${step}</span>
                            ${badgeHtml}
                        </div>
                    </div>
                `;
            }).join('');

            // Animation container
            const animationHtml = status === 'running'
                ? '<div id="loading-animation" class="live-status-animation"></div>'
                : `<div class="live-status-icon live-status-icon--success">
                        <svg class="live-status-checkmark" viewBox="0 0 24 24">
                            <path d="M4 12l5 5L20 6"/>
                        </svg>
                   </div>`;

            const statusTitle = status === 'running'
                ? 'Computing...'
                : 'Computation Complete!';

            const statusNote = status === 'running'
                ? 'Processing the data. Please wait while the computation is being performed.'
                : 'All computations have been successfully completed. Click the button below to view your results.';

            return `
                <div class="live-status-wrapper" role="region" aria-label="Computation Progress" aria-live="polite">
                    <div class="live-status-header">
                        ${animationHtml}
                        <div class="live-status-header-info">
                            <h3 class="live-status-title">${statusTitle}</h3>
                            <div class="live-status-timer" aria-label="Elapsed time">
                                <svg class="live-status-timer-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <circle cx="12" cy="12" r="10"/>
                                    <polyline points="12 6 12 12 16 14"/>
                                </svg>
                                <span>Elapsed:</span>
                                <span class="live-status-timer-value" id="elapsed-timer">${elapsedFormatted}</span>
                            </div>
                        </div>
                    </div>
                    <p class="live-status-note">${statusNote}</p>
                    <div class="live-status-timeline" role="list" aria-label="Computation steps">
                        ${stepsHtml || '<div class="live-status-step active"><div class="live-status-step-dot">1</div><div class="live-status-step-content"><span class="live-status-step-text">Initializing computation...</span><span class="live-status-badge live-status-badge--running">Starting</span></div></div>'}
                    </div>
                </div>
            `;
        };

        // Initialize loading animation
        const initLoadingAnimation = () => {
            const container = document.getElementById('loading-animation');
            if (!container) return;

            // Inject 3 pulsing dots animation
            container.innerHTML = `
                <div class="live-status-dots">
                    <div class="live-status-dot"></div>
                    <div class="live-status-dot"></div>
                    <div class="live-status-dot"></div>
                </div>
            `;
        };

        // Start elapsed timer update
        const startTimer = () => {
            timerInterval = setInterval(() => {
                const timerEl = document.getElementById('elapsed-timer');
                if (timerEl) {
                    const elapsedMs = performance.now() - streamStartedAt;
                    timerEl.textContent = formatElapsedTime(elapsedMs);
                }
            }, 50); // Update every 50ms for smooth display
        };

        // Stop timer
        const stopTimer = () => {
            if (timerInterval) {
                clearInterval(timerInterval);
                timerInterval = null;
            }
        };

        // Render modal with custom SweetAlert2 classes
        const renderModal = (status = 'running') => {
            const html = buildTimelineHtml(progressSteps, status);

            if (!Swal.isVisible()) {
                Swal.fire({
                    title: 'Computation Status',
                    html: html,
                    allowOutsideClick: false,
                    allowEscapeKey: false,
                    showConfirmButton: false,
                    customClass: {
                        popup: 'computation-modal',
                        title: 'computation-modal-title',
                        htmlContainer: 'computation-modal-content'
                    },
                    didOpen: () => {
                        initLoadingAnimation();
                        startTimer();
                        // Scroll timeline to bottom
                        const timeline = Swal.getHtmlContainer()?.querySelector('.live-status-timeline');
                        if (timeline) timeline.scrollTo({ top: timeline.scrollHeight, behavior: 'smooth' });
                    }
                });
            } else {
                Swal.update({ html });
                // Re-init animation after update if still running
                if (status === 'running') {
                    setTimeout(() => initLoadingAnimation(), 10);
                }
                // Scroll timeline to bottom
                const timeline = Swal.getHtmlContainer()?.querySelector('.live-status-timeline');
                if (timeline) timeline.scrollTo({ top: timeline.scrollHeight, behavior: 'smooth' });
            }
        };

        // Append new status step
        const appendStatus = (message) => {
            if (!message) return;
            progressSteps.push(message);
            lastProgressAt = performance.now();
            renderModal('running');
        };

        // Show completion modal with celebration
        const showCompletion = (redirectUrl, backendElapsedMs = null) => {
            // Stop timer immediately to prevent flickering
            stopTimer();

            // Immediately update the timer display with backend time if available
            if (backendElapsedMs !== null) {
                const timerEl = document.getElementById('elapsed-timer');
                if (timerEl) {
                    timerEl.textContent = formatElapsedTime(backendElapsedMs);
                }
            }

            const elapsed = performance.now() - streamStartedAt;
            const sinceLastProgress = performance.now() - lastProgressAt;
            const basicWait = Math.max(MIN_PROGRESS_MS - elapsed, 0);
            const afterProgressWait = Math.max(MIN_AFTER_LAST_PROGRESS_MS - sinceLastProgress, 0);
            const waitMs = Math.max(basicWait, afterProgressWait);

            setTimeout(() => {

                // Use backend elapsed time if available, otherwise use frontend time
                const completionHtml = buildTimelineHtml(progressSteps, 'success', backendElapsedMs);

                Swal.fire({
                    title: 'Computation Status',
                    html: completionHtml,
                    allowOutsideClick: false,
                    allowEscapeKey: false,
                    showConfirmButton: true,
                    confirmButtonText: 'Show Results',
                    focusConfirm: true,
                    customClass: {
                        popup: 'computation-modal',
                        title: 'computation-modal-title',
                        htmlContainer: 'computation-modal-content',
                        confirmButton: 'computation-modal-confirm'
                    },
                    didOpen: () => {
                        // Scroll timeline to bottom to show all completed steps
                        const timeline = Swal.getHtmlContainer()?.querySelector('.live-status-timeline');
                        if (timeline) timeline.scrollTo({ top: timeline.scrollHeight, behavior: 'smooth' });

                        // Add celebration particles
                        const wrapper = Swal.getHtmlContainer()?.querySelector('.live-status-wrapper');
                        if (wrapper) {
                            addCelebrationParticles(wrapper);
                        }
                    }
                }).then(() => {
                    window.location.href = redirectUrl;
                });
            }, waitMs);
        };

        // Add celebration particles on completion
        const addCelebrationParticles = (container) => {
            const celebration = document.createElement('div');
            celebration.className = 'live-status-celebration';
            container.style.position = 'relative';
            container.appendChild(celebration);

            const colors = ['#6366f1', '#22d3ee', '#34d399', '#f59e0b', '#ec4899'];
            const particleCount = 20;

            for (let i = 0; i < particleCount; i++) {
                const particle = document.createElement('div');
                particle.className = 'celebration-particle';
                particle.style.background = colors[Math.floor(Math.random() * colors.length)];
                particle.style.left = '50%';
                particle.style.top = '30%';

                // Random direction
                const angle = (Math.random() * 360) * (Math.PI / 180);
                const distance = 50 + Math.random() * 80;
                particle.style.setProperty('--tx', `${Math.cos(angle) * distance}px`);
                particle.style.setProperty('--ty', `${Math.sin(angle) * distance}px`);
                particle.style.animationDelay = `${Math.random() * 0.3}s`;

                celebration.appendChild(particle);
            }

            // Clean up particles after animation
            setTimeout(() => celebration.remove(), 1500);
        };

        // Build init params and request a short-lived token to avoid EventSource URLs
        const initParams = new URLSearchParams();
        initParams.set('manualData', manualData);
        if (fds) initParams.set('fds', fds);
        initParams.set('monteCarlo', monteCarloSelected ? 'true' : 'false');
        initParams.set('samples', samples || '100000');
        initParams.set('duplicatesRemoved', duplicatesRemoved || '0');
        initParams.set('mode', normalizedMode);

        fetch('/compute/stream-init', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
            body: initParams.toString()
        })
        .then(resp => {
            if (!resp.ok) throw new Error('Failed to initialize computation.');
            return resp.json();
        })
        .then(data => {
            const token = data && data.token;
            if (!token) throw new Error('Computation token is missing.');

            const source = new EventSource(`/compute/stream?token=${encodeURIComponent(token)}`);

            source.onopen = () => {
                // Connection opened - already showing initialization step
            };

            source.addEventListener('progress', event => {
                try {
                    const payload = JSON.parse(event.data);
                    appendStatus(payload.message);
                } catch (err) {
                    appendStatus(event.data);
                }
            });

            source.addEventListener('complete', event => {
                source.close();
                if (computeBtn) computeBtn.disabled = false;
                let data;
                try {
                    data = JSON.parse(event.data);
                } catch (err) {
                    data = {};
                }
                const redirectUrl = data && data.redirectUrl ? data.redirectUrl : '/calc-results';
                const backendElapsedMs = data && data.elapsedMs ? data.elapsedMs : null;
                showCompletion(redirectUrl, backendElapsedMs);
            });

            source.addEventListener('error', event => {
                stopTimer();

                let message = 'Unexpected error during computation.';
                try {
                    const payload = JSON.parse(event.data);
                    if (payload && payload.message) message = payload.message;
                } catch (err) {
                    if (event.data) message = event.data;
                }

                Swal.fire({
                    icon: 'error',
                    title: 'Computation Failed',
                    text: message,
                    confirmButtonText: 'Close',
                    customClass: {
                        popup: 'computation-modal'
                    }
                });
                if (computeBtn) computeBtn.disabled = false;
                source.close();
            });

            source.onerror = (event) => {
                // Only show error if not already closed by complete event
                if (source.readyState === EventSource.CLOSED) {
                    // Check if we should ignore this (might be expected after complete)
                    if (Swal.isVisible() && progressSteps.some(item => item.toLowerCase().includes('completed'))) {
                        return;
                    }
                }

                stopTimer();

                Swal.fire({
                    icon: 'error',
                    title: 'Connection Lost',
                    text: 'Connection lost while receiving computation updates. Please check if the server is running.',
                    confirmButtonText: 'Close',
                    customClass: {
                        popup: 'computation-modal'
                    }
                });
                if (computeBtn) computeBtn.disabled = false;
                source.close();
            };
        })
        .catch(err => {
            stopTimer();

            Swal.fire({
                icon: 'error',
                title: 'Initialization Failed',
                text: err && err.message ? err.message : 'Unable to start computation.',
                confirmButtonText: 'Close',
                customClass: {
                    popup: 'computation-modal'
                }
            });
            if (computeBtn) computeBtn.disabled = false;
        });
    }
});
