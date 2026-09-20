/**
 * StegoVault - Secret Message Encoder & Decoder Frontend Application
 * Supports dual execution:
 * 1. High-speed Java Spring Boot backend (when server is running)
 * 2. In-browser client-side Web Cryptography & HTML5 Canvas LSB engine
 *    (for seamless static deployment on Netlify / GitHub Pages)
 */

document.addEventListener('DOMContentLoaded', () => {
    // =========================================================================
    // 1. Application State & Storage
    // =========================================================================
    const state = {
        token: localStorage.getItem('stegovault_token') || null,
        user: JSON.parse(localStorage.getItem('stegovault_user') || 'null'),
        theme: localStorage.getItem('stegovault_theme') || 'dark',
        imageEncodeFile: null,
        imageDecodeFile: null,
        imageEncodeDims: { width: 0, height: 0 },
        historyRecords: []
    };

    const API_BASE = window.location.pathname.includes('/Stego_Vault') ? '/Stego_Vault/api' : '/api';

    // Set initial theme
    document.documentElement.setAttribute('data-theme', state.theme);

    // =========================================================================
    // 2. Toast Notifications
    // =========================================================================
    const toastContainer = document.getElementById('toast-container');

    function showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;

        const icons = {
            success: '✓',
            error: '✕',
            info: 'ℹ'
        };

        toast.innerHTML = `
            <span style="font-weight: bold;">${icons[type] || 'ℹ'}</span>
            <span>${escapeHtml(message)}</span>
        `;

        toastContainer.appendChild(toast);

        setTimeout(() => {
            toast.style.animation = 'toast-slide-out 0.3s forwards';
            setTimeout(() => {
                if (toast.parentNode) toast.parentNode.removeChild(toast);
            }, 300);
        }, 4200);
    }

    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    // =========================================================================
    // 3. Theme Toggle
    // =========================================================================
    const themeToggleBtn = document.getElementById('theme-toggle-btn');
    themeToggleBtn.addEventListener('click', () => {
        state.theme = state.theme === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', state.theme);
        localStorage.setItem('stegovault_theme', state.theme);
        showToast(`Switched to ${state.theme} mode`, 'info');
    });

    // =========================================================================
    // 4. Authentication State & UI
    // =========================================================================
    const authGuestControls = document.getElementById('auth-guest-controls');
    const authUserControls = document.getElementById('auth-user-controls');
    const userAvatar = document.getElementById('user-avatar');
    const userDisplayName = document.getElementById('user-display-name');
    const btnLogout = document.getElementById('btn-logout');

    function updateAuthUi() {
        if (state.token && state.user) {
            authGuestControls.style.display = 'none';
            authUserControls.style.display = 'flex';
            userDisplayName.textContent = state.user.name;
            userAvatar.textContent = state.user.name.charAt(0).toUpperCase();
        } else {
            authGuestControls.style.display = 'flex';
            authUserControls.style.display = 'none';
        }
    }

    btnLogout.addEventListener('click', () => {
        state.token = null;
        state.user = null;
        localStorage.removeItem('stegovault_token');
        localStorage.removeItem('stegovault_user');
        updateAuthUi();
        showToast('Logged out successfully', 'info');
        loadHistory();
    });

    function getAuthHeaders(headers = {}) {
        if (state.token) {
            headers['Authorization'] = `Bearer ${state.token}`;
        }
        return headers;
    }

    // Check existing token validity on startup
    if (state.token) {
        fetch(`${API_BASE}/auth/me`, { headers: getAuthHeaders() })
            .then(res => {
                if (!res.ok) throw new Error('Token expired');
                return res.json();
            })
            .then(data => {
                if (data.success && data.data) {
                    state.user = data.data;
                    localStorage.setItem('stegovault_user', JSON.stringify(state.user));
                    updateAuthUi();
                }
            })
            .catch(() => {
                // If offline or expired, preserve local user if present
                updateAuthUi();
            });
    } else {
        updateAuthUi();
    }

    // =========================================================================
    // 5. Navigation & View Tabs
    // =========================================================================
    const navTabs = document.querySelectorAll('.nav-tab');
    const viewSections = document.querySelectorAll('.view-section');

    navTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const targetViewId = tab.getAttribute('data-tab');

            navTabs.forEach(t => {
                t.classList.remove('active');
                t.setAttribute('aria-selected', 'false');
            });
            tab.classList.add('active');
            tab.setAttribute('aria-selected', 'true');

            viewSections.forEach(v => {
                v.style.display = 'none';
                v.classList.remove('active');
            });

            const targetView = document.getElementById(targetViewId);
            if (targetView) {
                targetView.style.display = 'block';
                targetView.classList.add('active');
            }

            if (targetViewId === 'history-view') {
                loadHistory();
            }
        });
    });

    // Sub-Tabs Handler (Hide / Extract within views)
    function setupSubTabs(containerSelector) {
        const subTabs = document.querySelectorAll(`${containerSelector} .sub-tab`);
        subTabs.forEach(tab => {
            tab.addEventListener('click', () => {
                const targetPanelId = tab.getAttribute('data-subtab');
                subTabs.forEach(t => t.classList.remove('active'));
                tab.classList.add('active');

                const parent = tab.closest('.view-section');
                parent.querySelectorAll('.subtab-panel').forEach(p => {
                    p.style.display = 'none';
                    p.classList.remove('active');
                });

                const targetPanel = document.getElementById(targetPanelId);
                if (targetPanel) {
                    targetPanel.style.display = 'block';
                    targetPanel.classList.add('active');
                }
            });
        });
    }

    setupSubTabs('#image-view');
    setupSubTabs('#text-view');

    // Password visibility toggle helpers
    document.querySelectorAll('.btn-toggle-password').forEach(btn => {
        btn.addEventListener('click', () => {
            const targetInputId = btn.getAttribute('data-target');
            const input = document.getElementById(targetInputId);
            if (input) {
                const isPassword = input.type === 'password';
                input.type = isPassword ? 'text' : 'password';
                btn.textContent = isPassword ? '🔒' : '👁️';
            }
        });
    });

    // Password strength meter
    function updatePasswordStrength(password, fillElem, labelElem) {
        if (!fillElem) return;
        let score = 0;
        if (password.length >= 8) score++;
        if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score++;
        if (/[0-9]/.test(password)) score++;
        if (/[^A-Za-z0-9]/.test(password)) score++;

        const colors = ['#ef4444', '#f59e0b', '#3b82f6', '#10b981'];
        const labels = ['Weak', 'Moderate', 'Strong', 'Very Strong'];
        const widths = ['25%', '50%', '75%', '100%'];

        if (password.length === 0) {
            fillElem.style.width = '0%';
            if (labelElem) labelElem.textContent = 'Security: None';
        } else {
            const idx = Math.max(0, score - 1);
            fillElem.style.width = widths[idx];
            fillElem.style.background = colors[idx];
            if (labelElem) labelElem.textContent = `Security: ${labels[idx]}`;
        }
    }

    const imageEncodePasswordInput = document.getElementById('image-encode-password');
    const imagePasswordStrengthFill = document.getElementById('image-password-strength-fill');
    const imagePasswordStrengthLabel = document.getElementById('image-password-strength-label');
    imageEncodePasswordInput.addEventListener('input', () => {
        updatePasswordStrength(imageEncodePasswordInput.value, imagePasswordStrengthFill, imagePasswordStrengthLabel);
    });

    const regPasswordInput = document.getElementById('reg-password');
    const regPasswordStrengthFill = document.getElementById('reg-password-strength-fill');
    const regPasswordStrengthLabel = document.getElementById('reg-password-strength-label');
    regPasswordInput.addEventListener('input', () => {
        updatePasswordStrength(regPasswordInput.value, regPasswordStrengthFill, regPasswordStrengthLabel);
    });

    // =========================================================================
    // 6. Cryptographic Engine (Web Crypto API Fallback)
    // =========================================================================
    const MAGIC_HEADER = [0x53, 0x54, 0x45, 0x47]; // "STEG"
    const PROTOCOL_VERSION = 0x01;
    const ZW_ZERO = '\u200B';
    const ZW_ONE = '\u200C';
    const ZW_START = '\u200D';
    const ZW_END = '\uFEFF';

    async function clientDeriveKey(password, salt) {
        const enc = new TextEncoder();
        const keyMaterial = await window.crypto.subtle.importKey(
            "raw",
            enc.encode(password),
            { name: "PBKDF2" },
            false,
            ["deriveKey"]
        );
        return await window.crypto.subtle.deriveKey(
            {
                name: "PBKDF2",
                salt: salt,
                iterations: 65536,
                hash: "SHA-256"
            },
            keyMaterial,
            { name: "AES-GCM", length: 256 },
            false,
            ["encrypt", "decrypt"]
        );
    }

    async function clientAesEncrypt(plaintextBytes, password) {
        const salt = window.crypto.getRandomValues(new Uint8Array(16));
        const iv = window.crypto.getRandomValues(new Uint8Array(12));
        const key = await clientDeriveKey(password, salt);
        const ciphertextBuffer = await window.crypto.subtle.encrypt(
            { name: "AES-GCM", iv: iv, tagLength: 128 },
            key,
            plaintextBytes
        );
        return { salt, iv, ciphertext: new Uint8Array(ciphertextBuffer) };
    }

    async function clientAesDecrypt(salt, iv, ciphertext, password) {
        try {
            const key = await clientDeriveKey(password, salt);
            const decrypted = await window.crypto.subtle.decrypt(
                { name: "AES-GCM", iv: iv, tagLength: 128 },
                key,
                ciphertext
            );
            return new Uint8Array(decrypted);
        } catch (e) {
            throw new Error("Unable to decrypt the hidden message.");
        }
    }

    function recordLocalHistory(type, fileName, fileType, status) {
        const local = JSON.parse(localStorage.getItem('stegovault_local_history') || '[]');
        local.unshift({
            id: Date.now(),
            operationType: type,
            fileName: fileName || 'unnamed',
            fileType: fileType || 'data',
            status: status,
            createdAt: new Date().toISOString()
        });
        localStorage.setItem('stegovault_local_history', JSON.stringify(local.slice(0, 50)));
    }

    // =========================================================================
    // 7. Image Steganography: Live Capacity Checker & Dropzone
    // =========================================================================
    const imageEncodeDropzone = document.getElementById('image-encode-dropzone');
    const imageEncodeInput = document.getElementById('image-encode-input');
    const imageEncodeEmptyPrompt = document.getElementById('image-encode-empty-prompt');
    const imageEncodePreviewContainer = document.getElementById('image-encode-preview-container');
    const imageEncodePreview = document.getElementById('image-encode-preview');
    const imageEncodeFileName = document.getElementById('image-encode-file-name');
    const imageEncodeFileDims = document.getElementById('image-encode-file-dims');
    const imageEncodeRemoveBtn = document.getElementById('image-encode-remove-btn');

    const imageEncodeMessage = document.getElementById('image-encode-message');
    const imageEncodeCharCount = document.getElementById('image-encode-char-count');

    const capacityBadge = document.getElementById('capacity-badge');
    const capacityMaxVal = document.getElementById('capacity-max-val');
    const capacityReqVal = document.getElementById('capacity-req-val');
    const capacityProgressFill = document.getElementById('capacity-progress-fill');
    const capacityStatusText = document.getElementById('capacity-status-text');
    const btnSubmitImageEncode = document.getElementById('btn-submit-image-encode');

    function calculateCapacity() {
        const { width, height } = state.imageEncodeDims;
        const message = imageEncodeMessage.value;
        const msgBytes = new TextEncoder().encode(message).length;
        imageEncodeCharCount.textContent = `${message.length} characters (${msgBytes} bytes)`;

        if (!state.imageEncodeFile || width === 0 || height === 0) {
            capacityBadge.textContent = 'Awaiting Image';
            capacityBadge.className = 'capacity-badge';
            capacityMaxVal.textContent = '0 KB';
            capacityReqVal.textContent = '0 KB';
            capacityProgressFill.style.width = '0%';
            capacityProgressFill.className = 'progress-bar-fill';
            capacityStatusText.textContent = 'Please select a PNG carrier image to analyze capacity.';
            btnSubmitImageEncode.disabled = false;
            return;
        }

        const maxCapacityBytes = Math.floor((width * height * 3) / 8);
        const maxCapacityKb = (maxCapacityBytes / 1024).toFixed(1);

        const requiredBytes = 53 + msgBytes;
        const requiredKb = (requiredBytes / 1024).toFixed(2);
        const percentage = Math.min(100, ((requiredBytes / maxCapacityBytes) * 100)).toFixed(1);

        capacityMaxVal.textContent = `${maxCapacityKb} KB (${maxCapacityBytes.toLocaleString()} B)`;
        capacityReqVal.textContent = `${requiredKb} KB (${requiredBytes.toLocaleString()} B)`;
        capacityProgressFill.style.width = `${percentage}%`;

        if (requiredBytes <= maxCapacityBytes) {
            if (percentage > 85) {
                capacityBadge.textContent = `${percentage}% Full`;
                capacityBadge.className = 'capacity-badge danger';
                capacityProgressFill.className = 'progress-bar-fill warning';
                capacityStatusText.innerHTML = `⚠️ <strong>Approaching capacity:</strong> Image can store this message (${percentage}% used).`;
            } else {
                capacityBadge.textContent = `${percentage}% Used`;
                capacityBadge.className = 'capacity-badge ready';
                capacityProgressFill.className = 'progress-bar-fill';
                capacityStatusText.innerHTML = `✓ <strong>Image can store this message</strong> (${percentage}% capacity utilized).`;
            }
            btnSubmitImageEncode.disabled = false;
        } else {
            capacityBadge.textContent = 'Capacity Exceeded';
            capacityBadge.className = 'capacity-badge danger';
            capacityProgressFill.className = 'progress-bar-fill danger';
            capacityStatusText.innerHTML = `❌ <strong>Message is too large for this image.</strong> Please choose a larger image.`;
            btnSubmitImageEncode.disabled = true;
        }
    }

    imageEncodeMessage.addEventListener('input', calculateCapacity);

    function handleImageEncodeSelect(file) {
        if (!file) return;
        if (!file.type.includes('png') && !file.name.toLowerCase().endsWith('.png')) {
            showToast('Unsupported format: Please select a valid PNG image.', 'error');
            return;
        }

        state.imageEncodeFile = file;
        imageEncodeFileName.textContent = file.name;

        const reader = new FileReader();
        reader.onload = (e) => {
            const img = new Image();
            img.onload = () => {
                state.imageEncodeDims = { width: img.naturalWidth, height: img.naturalHeight };
                imageEncodeFileDims.textContent = `${img.naturalWidth} × ${img.naturalHeight} px`;
                imageEncodePreview.src = e.target.result;
                imageEncodeEmptyPrompt.style.display = 'none';
                imageEncodePreviewContainer.style.display = 'flex';
                calculateCapacity();
                showToast(`Loaded ${file.name} (${img.naturalWidth}x${img.naturalHeight})`, 'info');
            };
            img.src = e.target.result;
        };
        reader.readAsDataURL(file);
    }

    imageEncodeDropzone.addEventListener('click', (e) => {
        if (e.target !== imageEncodeRemoveBtn) {
            imageEncodeInput.click();
        }
    });

    imageEncodeInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            handleImageEncodeSelect(e.target.files[0]);
        }
    });

    imageEncodeRemoveBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        state.imageEncodeFile = null;
        state.imageEncodeDims = { width: 0, height: 0 };
        imageEncodeInput.value = '';
        imageEncodeEmptyPrompt.style.display = 'block';
        imageEncodePreviewContainer.style.display = 'none';
        calculateCapacity();
    });

    ['dragenter', 'dragover'].forEach(eventName => {
        imageEncodeDropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            imageEncodeDropzone.classList.add('dragover');
        });
    });

    ['dragleave', 'drop'].forEach(eventName => {
        imageEncodeDropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            imageEncodeDropzone.classList.remove('dragover');
        });
    });

    imageEncodeDropzone.addEventListener('drop', (e) => {
        const dt = e.dataTransfer;
        const files = dt.files;
        if (files.length > 0) {
            handleImageEncodeSelect(files[0]);
        }
    });

    // =========================================================================
    // 8. Image Encode Action (Backend API + Web Crypto LSB Fallback)
    // =========================================================================
    const formImageEncode = document.getElementById('form-image-encode');
    const imageEncodeResult = document.getElementById('image-encode-result');
    const imageEncodeResultImg = document.getElementById('image-encode-result-img');
    const imageEncodeDownloadBtn = document.getElementById('image-encode-download-btn');

    async function encodeImageClientSide(imageFile, message, password) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = async (e) => {
                const img = new Image();
                img.onload = async () => {
                    try {
                        const canvas = document.createElement('canvas');
                        canvas.width = img.naturalWidth;
                        canvas.height = img.naturalHeight;
                        const ctx = canvas.getContext('2d');
                        ctx.drawImage(img, 0, 0);

                        const imgData = ctx.getImageData(0, 0, canvas.width, canvas.height);
                        const data = imgData.data;

                        const messageBytes = new TextEncoder().encode(message);
                        const encrypted = await clientAesEncrypt(messageBytes, password);

                        // Assemble payload
                        const payloadLen = 37 + encrypted.ciphertext.length;
                        const payload = new Uint8Array(payloadLen);
                        payload.set(MAGIC_HEADER, 0);
                        payload[4] = PROTOCOL_VERSION;
                        payload.set(encrypted.salt, 5);
                        payload.set(encrypted.iv, 21);

                        // 4 bytes big endian length
                        const len = encrypted.ciphertext.length;
                        payload[33] = (len >> 24) & 0xFF;
                        payload[34] = (len >> 16) & 0xFF;
                        payload[35] = (len >> 8) & 0xFF;
                        payload[36] = len & 0xFF;

                        payload.set(encrypted.ciphertext, 37);

                        // Embed LSBs into R, G, B
                        let pByte = 0;
                        let pBit = 0;
                        const totalBytes = payload.length;

                        for (let i = 0; i < data.length; i += 4) {
                            if (pByte >= totalBytes) break;

                            for (let ch = 0; ch < 3; ch++) {
                                if (pByte < totalBytes) {
                                    const bit = (payload[pByte] >> (7 - pBit)) & 1;
                                    data[i + ch] = (data[i + ch] & 0xFE) | bit;
                                    pBit++;
                                    if (pBit === 8) {
                                        pBit = 0;
                                        pByte++;
                                    }
                                }
                            }
                        }

                        ctx.putImageData(imgData, 0, 0);
                        canvas.toBlob((blob) => {
                            if (blob) resolve(blob);
                            else reject(new Error('Canvas toBlob failed'));
                        }, 'image/png');
                    } catch (err) {
                        reject(err);
                    }
                };
                img.src = e.target.result;
            };
            reader.readAsDataURL(imageFile);
        });
    }

    formImageEncode.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!state.imageEncodeFile) {
            showToast('Please upload a PNG carrier image first.', 'error');
            return;
        }

        const message = imageEncodeMessage.value.trim();
        const password = imageEncodePasswordInput.value;

        if (!message) {
            showToast('Secret message cannot be empty.', 'error');
            return;
        }
        if (!password) {
            showToast('Please provide an encryption password.', 'error');
            return;
        }

        btnSubmitImageEncode.disabled = true;
        btnSubmitImageEncode.innerHTML = '<span class="btn-icon">⏳</span> Encrypting &amp; Embedding...';

        try {
            let blob = null;

            // Attempt backend API first
            try {
                const formData = new FormData();
                formData.append('image', state.imageEncodeFile);
                formData.append('message', message);
                formData.append('password', password);

                const response = await fetch(`${API_BASE}/steganography/image/encode`, {
                    method: 'POST',
                    headers: getAuthHeaders(),
                    body: formData
                });

                if (response.ok) {
                    blob = await response.blob();
                }
            } catch (netErr) {
                // Backend unavailable, fallback to client-side
            }

            if (!blob) {
                blob = await encodeImageClientSide(state.imageEncodeFile, message, password);
                recordLocalHistory('IMAGE_ENCODE', state.imageEncodeFile.name, 'image/png', 'COMPLETED');
            }

            const downloadUrl = URL.createObjectURL(blob);
            imageEncodeResultImg.src = downloadUrl;
            const originalName = state.imageEncodeFile.name.replace(/\.[^/.]+$/, "");
            imageEncodeDownloadBtn.href = downloadUrl;
            imageEncodeDownloadBtn.download = `stego-${originalName}.png`;

            imageEncodeResult.style.display = 'block';
            imageEncodeResult.scrollIntoView({ behavior: 'smooth' });
            showToast('Secret message embedded successfully! Click Download to save.', 'success');
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btnSubmitImageEncode.disabled = false;
            btnSubmitImageEncode.innerHTML = '<span class="btn-icon">🔒</span> Encrypt &amp; Hide Message';
        }
    });

    // =========================================================================
    // 9. Image Decode Action (Backend API + Web Crypto LSB Fallback)
    // =========================================================================
    const imageDecodeDropzone = document.getElementById('image-decode-dropzone');
    const imageDecodeInput = document.getElementById('image-decode-input');
    const imageDecodeEmptyPrompt = document.getElementById('image-decode-empty-prompt');
    const imageDecodePreviewContainer = document.getElementById('image-decode-preview-container');
    const imageDecodePreview = document.getElementById('image-decode-preview');
    const imageDecodeFileName = document.getElementById('image-decode-file-name');
    const imageDecodeRemoveBtn = document.getElementById('image-decode-remove-btn');

    function handleImageDecodeSelect(file) {
        if (!file) return;
        if (!file.type.includes('png') && !file.name.toLowerCase().endsWith('.png')) {
            showToast('Unsupported format: Please upload an encoded PNG image.', 'error');
            return;
        }

        state.imageDecodeFile = file;
        imageDecodeFileName.textContent = file.name;

        const reader = new FileReader();
        reader.onload = (e) => {
            imageDecodePreview.src = e.target.result;
            imageDecodeEmptyPrompt.style.display = 'none';
            imageDecodePreviewContainer.style.display = 'flex';
            showToast(`Loaded encoded image: ${file.name}`, 'info');
        };
        reader.readAsDataURL(file);
    }

    imageDecodeDropzone.addEventListener('click', (e) => {
        if (e.target !== imageDecodeRemoveBtn) {
            imageDecodeInput.click();
        }
    });

    imageDecodeInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            handleImageDecodeSelect(e.target.files[0]);
        }
    });

    imageDecodeRemoveBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        state.imageDecodeFile = null;
        imageDecodeInput.value = '';
        imageDecodeEmptyPrompt.style.display = 'block';
        imageDecodePreviewContainer.style.display = 'none';
    });

    ['dragenter', 'dragover'].forEach(eventName => {
        imageDecodeDropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            imageDecodeDropzone.classList.add('dragover');
        });
    });

    ['dragleave', 'drop'].forEach(eventName => {
        imageDecodeDropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            imageDecodeDropzone.classList.remove('dragover');
        });
    });

    imageDecodeDropzone.addEventListener('drop', (e) => {
        const dt = e.dataTransfer;
        if (dt.files.length > 0) {
            handleImageDecodeSelect(dt.files[0]);
        }
    });

    const formImageDecode = document.getElementById('form-image-decode');
    const imageDecodePassword = document.getElementById('image-decode-password');
    const btnSubmitImageDecode = document.getElementById('btn-submit-image-decode');
    const imageDecodeResult = document.getElementById('image-decode-result');
    const imageDecodeMessageOutput = document.getElementById('image-decode-message-output');
    const btnCopyImageDecoded = document.getElementById('btn-copy-image-decoded');

    async function decodeImageClientSide(imageFile, password) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = async (e) => {
                const img = new Image();
                img.onload = async () => {
                    try {
                        const canvas = document.createElement('canvas');
                        canvas.width = img.naturalWidth;
                        canvas.height = img.naturalHeight;
                        const ctx = canvas.getContext('2d');
                        ctx.drawImage(img, 0, 0);

                        const imgData = ctx.getImageData(0, 0, canvas.width, canvas.height);
                        const data = imgData.data;

                        // Bit reader
                        let pByte = 0;
                        let pBit = 0;
                        let curByte = 0;
                        const bytes = [];

                        function readNextBytes(count) {
                            const result = new Uint8Array(count);
                            let rIdx = 0;
                            while (rIdx < count) {
                                // iterate through pixels
                                for (let i = 0; i < data.length && rIdx < count; i += 4) {
                                    for (let ch = 0; ch < 3 && rIdx < count; ch++) {
                                        const bit = data[i + ch] & 1;
                                        curByte = (curByte << 1) | bit;
                                        pBit++;
                                        if (pBit === 8) {
                                            result[rIdx++] = curByte;
                                            curByte = 0;
                                            pBit = 0;
                                        }
                                    }
                                }
                            }
                            return result;
                        }

                        // Read header (37 bytes)
                        const header = readNextBytes(37);

                        // Verify magic
                        if (header[0] !== 0x53 || header[1] !== 0x54 || header[2] !== 0x45 || header[3] !== 0x47) {
                            throw new Error("Unable to decrypt the hidden message.");
                        }

                        const salt = header.slice(5, 21);
                        const iv = header.slice(21, 33);
                        const cipherLen = (header[33] << 24) | (header[34] << 16) | (header[35] << 8) | header[36];

                        if (cipherLen <= 0 || cipherLen > (data.length / 4)) {
                            throw new Error("Unable to decrypt the hidden message.");
                        }

                        // Read all bits to retrieve ciphertext
                        const totalBytesNeeded = 37 + cipherLen;
                        const fullPayload = new Uint8Array(totalBytesNeeded);
                        let fIdx = 0;
                        curByte = 0;
                        pBit = 0;

                        outer:
                        for (let i = 0; i < data.length; i += 4) {
                            for (let ch = 0; ch < 3; ch++) {
                                const bit = data[i + ch] & 1;
                                curByte = (curByte << 1) | bit;
                                pBit++;
                                if (pBit === 8) {
                                    fullPayload[fIdx++] = curByte;
                                    curByte = 0;
                                    pBit = 0;
                                    if (fIdx >= totalBytesNeeded) break outer;
                                }
                            }
                        }

                        const ciphertext = fullPayload.slice(37, 37 + cipherLen);
                        const decrypted = await clientAesDecrypt(salt, iv, ciphertext, password);
                        resolve(new TextDecoder().decode(decrypted));
                    } catch (err) {
                        reject(new Error("Unable to decrypt the hidden message."));
                    }
                };
                img.src = e.target.result;
            };
            reader.readAsDataURL(imageFile);
        });
    }

    formImageDecode.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!state.imageDecodeFile) {
            showToast('Please upload an encoded PNG image first.', 'error');
            return;
        }

        const password = imageDecodePassword.value;
        if (!password) {
            showToast('Please enter the decryption password.', 'error');
            return;
        }

        btnSubmitImageDecode.disabled = true;
        btnSubmitImageDecode.innerHTML = '<span class="btn-icon">⚡</span> Extracting &amp; Decrypting...';

        try {
            let secretMessage = null;

            // Try backend API first
            try {
                const formData = new FormData();
                formData.append('image', state.imageDecodeFile);
                formData.append('password', password);

                const response = await fetch(`${API_BASE}/steganography/image/decode`, {
                    method: 'POST',
                    headers: getAuthHeaders(),
                    body: formData
                });

                if (response.ok) {
                    const data = await response.json();
                    if (data.success) {
                        secretMessage = data.data.secretMessage;
                    }
                }
            } catch (netErr) {
                // Backend unavailable
            }

            if (!secretMessage) {
                secretMessage = await decodeImageClientSide(state.imageDecodeFile, password);
                recordLocalHistory('IMAGE_DECODE', state.imageDecodeFile.name, 'image/png', 'COMPLETED');
            }

            imageDecodeMessageOutput.textContent = secretMessage;
            imageDecodeResult.style.display = 'block';
            imageDecodeResult.scrollIntoView({ behavior: 'smooth' });
            showToast('Secret message decrypted successfully!', 'success');
        } catch (err) {
            imageDecodeResult.style.display = 'none';
            showToast(`❌ ${err.message}`, 'error');
        } finally {
            btnSubmitImageDecode.disabled = false;
            btnSubmitImageDecode.innerHTML = '<span class="btn-icon">⚡</span> Extract &amp; Decrypt Secret Message';
        }
    });

    btnCopyImageDecoded.addEventListener('click', () => {
        const text = imageDecodeMessageOutput.textContent;
        navigator.clipboard.writeText(text).then(() => {
            showToast('Secret message copied to clipboard!', 'success');
        }).catch(() => {
            showToast('Failed to copy to clipboard', 'error');
        });
    });

    // =========================================================================
    // 10. Text Steganography (Backend API + Web Crypto Zero-Width Fallback)
    // =========================================================================
    const btnGenCoverText = document.getElementById('btn-gen-cover-text');
    const textEncodeCover = document.getElementById('text-encode-cover');
    const textEncodeSecret = document.getElementById('text-encode-secret');
    const textEncodePassword = document.getElementById('text-encode-password');
    const formTextEncode = document.getElementById('form-text-encode');
    const btnSubmitTextEncode = document.getElementById('btn-submit-text-encode');

    const textEncodeResultEmpty = document.getElementById('text-encode-result-empty');
    const textEncodeResultContent = document.getElementById('text-encode-result-content');
    const textEncodeOutput = document.getElementById('text-encode-output');
    const textEncodeStatBadge = document.getElementById('text-encode-stat-badge');
    const btnCopyTextEncoded = document.getElementById('btn-copy-text-encoded');
    const btnDownloadTextEncoded = document.getElementById('btn-download-text-encoded');

    const sampleCoverTexts = [
        "The project development sprint is on schedule. Please review the updated documentation before our afternoon sync.",
        "Today's technical architecture session has concluded. The team has finalized the protocol specifications for release.",
        "The weather across the central campus today is exceptionally clear, with optimal conditions for the outdoor summit.",
        "Quarterly budget reconciliation reports have been compiled and verified by the internal compliance review committee."
    ];

    btnGenCoverText.addEventListener('click', () => {
        const randomText = sampleCoverTexts[Math.floor(Math.random() * sampleCoverTexts.length)];
        textEncodeCover.value = randomText;
        showToast('Generated realistic carrier text', 'info');
    });

    async function encodeTextClientSide(coverText, secretMessage, password) {
        const carrier = (coverText && coverText.trim()) ? coverText.trim() : sampleCoverTexts[0];
        const messageBytes = new TextEncoder().encode(secretMessage);
        const encrypted = await clientAesEncrypt(messageBytes, password);

        // Pack: [Salt 16B] + [IV 12B] + [Ciphertext]
        const packed = new Uint8Array(16 + 12 + encrypted.ciphertext.length);
        packed.set(encrypted.salt, 0);
        packed.set(encrypted.iv, 16);
        packed.set(encrypted.ciphertext, 28);

        let zwSeq = ZW_START;
        for (let b of packed) {
            for (let i = 7; i >= 0; i--) {
                const bit = (b >> i) & 1;
                zwSeq += (bit === 1 ? ZW_ONE : ZW_ZERO);
            }
        }
        zwSeq += ZW_END;

        const firstSpace = carrier.indexOf(' ');
        const finalCover = firstSpace !== -1
            ? carrier.substring(0, firstSpace + 1) + zwSeq + carrier.substring(firstSpace + 1)
            : carrier + zwSeq;

        return { encodedText: finalCover, hiddenPayloadBytes: packed.length };
    }

    formTextEncode.addEventListener('submit', async (e) => {
        e.preventDefault();

        const coverText = textEncodeCover.value.trim();
        const secretMessage = textEncodeSecret.value.trim();
        const password = textEncodePassword.value;

        if (!secretMessage) {
            showToast('Secret message cannot be empty', 'error');
            return;
        }
        if (!password) {
            showToast('Encryption password is required', 'error');
            return;
        }

        btnSubmitTextEncode.disabled = true;
        btnSubmitTextEncode.innerHTML = '<span class="btn-icon">⏳</span> Encoding &amp; Hiding...';

        try {
            let result = null;

            // Try backend API first
            try {
                const response = await fetch(`${API_BASE}/steganography/text/encode`, {
                    method: 'POST',
                    headers: getAuthHeaders({ 'Content-Type': 'application/json' }),
                    body: JSON.stringify({ coverText, secretMessage, password })
                });

                if (response.ok) {
                    const data = await response.json();
                    if (data.success) {
                        result = data.data;
                    }
                }
            } catch (netErr) {
                // Backend offline
            }

            if (!result) {
                result = await encodeTextClientSide(coverText, secretMessage, password);
                recordLocalHistory('TEXT_ENCODE', 'secret-text.txt', 'text/plain', 'COMPLETED');
            }

            textEncodeOutput.value = result.encodedText;
            textEncodeStatBadge.textContent = `${result.hiddenPayloadBytes} B Encrypted Payload`;
            textEncodeResultEmpty.style.display = 'none';
            textEncodeResultContent.style.display = 'block';
            showToast('Secret message invisibly embedded into text!', 'success');
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btnSubmitTextEncode.disabled = false;
            btnSubmitTextEncode.innerHTML = '<span class="btn-icon">⚡</span> Encode Secret into Text';
        }
    });

    btnCopyTextEncoded.addEventListener('click', () => {
        navigator.clipboard.writeText(textEncodeOutput.value).then(() => {
            showToast('Encoded carrier text copied to clipboard!', 'success');
        }).catch(() => {
            showToast('Could not copy to clipboard', 'error');
        });
    });

    btnDownloadTextEncoded.addEventListener('click', () => {
        const blob = new Blob([textEncodeOutput.value], { type: 'text/plain;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'secret-message.txt';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        showToast('Downloaded secret-message.txt', 'info');
    });

    // Text Decode
    const formTextDecode = document.getElementById('form-text-decode');
    const textDecodeInput = document.getElementById('text-decode-input');
    const textDecodePassword = document.getElementById('text-decode-password');
    const textDecodeFileInput = document.getElementById('text-decode-file-input');
    const btnSubmitTextDecode = document.getElementById('btn-submit-text-decode');

    const textDecodeResultEmpty = document.getElementById('text-decode-result-empty');
    const textDecodeResultContent = document.getElementById('text-decode-result-content');
    const textDecodeOutput = document.getElementById('text-decode-output');
    const btnCopyTextDecoded = document.getElementById('btn-copy-text-decoded');

    textDecodeFileInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = (event) => {
            textDecodeInput.value = event.target.result;
            showToast(`Loaded ${file.name} into text box`, 'info');
        };
        reader.readAsText(file);
    });

    async function decodeTextClientSide(encodedText, password) {
        const start = encodedText.indexOf(ZW_START);
        const end = encodedText.indexOf(ZW_END, start + 1);

        if (start === -1 || end === -1 || end <= start + 1) {
            throw new Error("Unable to decrypt the hidden message.");
        }

        const bytes = [];
        let curByte = 0;
        let bits = 0;

        for (let i = start + 1; i < end; i++) {
            const ch = encodedText.charAt(i);
            if (ch === ZW_ZERO) {
                curByte = (curByte << 1);
                bits++;
            } else if (ch === ZW_ONE) {
                curByte = (curByte << 1) | 1;
                bits++;
            }
            if (bits === 8) {
                bytes.push(curByte);
                curByte = 0;
                bits = 0;
            }
        }

        if (bytes.length < 28 + 16) {
            throw new Error("Unable to decrypt the hidden message.");
        }

        const packed = new Uint8Array(bytes);
        const salt = packed.slice(0, 16);
        const iv = packed.slice(16, 28);
        const ciphertext = packed.slice(28);

        const decrypted = await clientAesDecrypt(salt, iv, ciphertext, password);
        return new TextDecoder().decode(decrypted);
    }

    formTextDecode.addEventListener('submit', async (e) => {
        e.preventDefault();

        const encodedText = textDecodeInput.value;
        const password = textDecodePassword.value;

        if (!encodedText) {
            showToast('Please paste the encoded text first', 'error');
            return;
        }
        if (!password) {
            showToast('Please enter the decryption password', 'error');
            return;
        }

        btnSubmitTextDecode.disabled = true;
        btnSubmitTextDecode.innerHTML = '<span class="btn-icon">⚡</span> Extracting &amp; Decrypting...';

        try {
            let secretMessage = null;

            // Try backend API first
            try {
                const response = await fetch(`${API_BASE}/steganography/text/decode`, {
                    method: 'POST',
                    headers: getAuthHeaders({ 'Content-Type': 'application/json' }),
                    body: JSON.stringify({ encodedText, password })
                });

                if (response.ok) {
                    const data = await response.json();
                    if (data.success) {
                        secretMessage = data.data.secretMessage;
                    }
                }
            } catch (netErr) {
                // Backend offline
            }

            if (!secretMessage) {
                secretMessage = await decodeTextClientSide(encodedText, password);
                recordLocalHistory('TEXT_DECODE', 'secret-text.txt', 'text/plain', 'COMPLETED');
            }

            textDecodeOutput.textContent = secretMessage;
            textDecodeResultEmpty.style.display = 'none';
            textDecodeResultContent.style.display = 'block';
            showToast('Secret message extracted and decrypted!', 'success');
        } catch (err) {
            textDecodeResultContent.style.display = 'none';
            textDecodeResultEmpty.style.display = 'block';
            showToast(`❌ ${err.message}`, 'error');
        } finally {
            btnSubmitTextDecode.disabled = false;
            btnSubmitTextDecode.innerHTML = '<span class="btn-icon">🔓</span> Extract &amp; Decrypt Secret Message';
        }
    });

    btnCopyTextDecoded.addEventListener('click', () => {
        navigator.clipboard.writeText(textDecodeOutput.textContent).then(() => {
            showToast('Decrypted message copied to clipboard!', 'success');
        }).catch(() => {
            showToast('Failed to copy to clipboard', 'error');
        });
    });

    // =========================================================================
    // 11. History Management
    // =========================================================================
    const historyLoginNotice = document.getElementById('history-login-notice');
    const historyContent = document.getElementById('history-content');
    const historyTableBody = document.getElementById('history-table-body');
    const historyEmptyState = document.getElementById('history-empty-state');
    const historySearchInput = document.getElementById('history-search-input');
    const historyFilterType = document.getElementById('history-filter-type');
    const btnRefreshHistory = document.getElementById('btn-refresh-history');

    async function loadHistory() {
        if (state.token) {
            historyLoginNotice.style.display = 'none';
            historyContent.style.display = 'block';

            try {
                const response = await fetch(`${API_BASE}/history`, {
                    headers: getAuthHeaders()
                });

                if (response.ok) {
                    const data = await response.json();
                    state.historyRecords = data.data || [];
                    renderHistoryTable();
                    return;
                }
            } catch (err) {
                // fallback to local
            }
        }

        // Show local operations history if guest or offline
        const local = JSON.parse(localStorage.getItem('stegovault_local_history') || '[]');
        if (local.length > 0) {
            historyLoginNotice.style.display = 'none';
            historyContent.style.display = 'block';
            state.historyRecords = local;
            renderHistoryTable();
        } else if (!state.token) {
            historyLoginNotice.style.display = 'block';
            historyContent.style.display = 'none';
        }
    }

    function renderHistoryTable() {
        const searchTerm = (historySearchInput.value || '').toLowerCase().trim();
        const filterType = historyFilterType.value;

        const filtered = state.historyRecords.filter(item => {
            const matchSearch = (item.fileName || '').toLowerCase().includes(searchTerm) ||
                                (item.operationType || '').toLowerCase().includes(searchTerm);
            const matchType = filterType === 'ALL' || item.operationType === filterType;
            return matchSearch && matchType;
        });

        historyTableBody.innerHTML = '';

        if (filtered.length === 0) {
            historyEmptyState.style.display = 'block';
            return;
        }
        historyEmptyState.style.display = 'none';

        const opLabels = {
            IMAGE_ENCODE: { icon: '🖼️🔒', label: 'Image Encode' },
            IMAGE_DECODE: { icon: '🖼️🔓', label: 'Image Decode' },
            TEXT_ENCODE: { icon: '📝🔒', label: 'Text Encode' },
            TEXT_DECODE: { icon: '📝🔓', label: 'Text Decode' }
        };

        filtered.forEach(item => {
            const tr = document.createElement('tr');
            const op = opLabels[item.operationType] || { icon: '⚙️', label: item.operationType };
            const statusClass = item.status === 'COMPLETED' ? 'completed' : 'failed';
            const statusText = item.status === 'COMPLETED' ? '✓ Completed' : '✕ Failed';
            const dateStr = item.createdAt ? new Date(item.createdAt).toLocaleString() : 'Recent';

            tr.innerHTML = `
                <td><strong>${op.icon} ${op.label}</strong></td>
                <td><code>${escapeHtml(item.fileName)}</code></td>
                <td><span class="card-tag">${escapeHtml(item.fileType)}</span></td>
                <td>${dateStr}</td>
                <td><span class="status-badge ${statusClass}">${statusText}</span></td>
                <td class="text-right">
                    <button class="btn btn-danger btn-xs btn-delete-history" data-id="${item.id}" title="Delete Record">
                        🗑️ Delete
                    </button>
                </td>
            `;

            historyTableBody.appendChild(tr);
        });

        document.querySelectorAll('.btn-delete-history').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.getAttribute('data-id');
                if (!confirm('Are you sure you want to delete this history record?')) return;

                if (state.token) {
                    try {
                        const res = await fetch(`${API_BASE}/history/${id}`, {
                            method: 'DELETE',
                            headers: getAuthHeaders()
                        });
                        if (res.ok) {
                            showToast('History record deleted', 'success');
                            loadHistory();
                            return;
                        }
                    } catch (e) {}
                }

                // Delete local record
                const local = JSON.parse(localStorage.getItem('stegovault_local_history') || '[]');
                const updated = local.filter(r => String(r.id) !== String(id));
                localStorage.setItem('stegovault_local_history', JSON.stringify(updated));
                showToast('History record deleted', 'success');
                loadHistory();
            });
        });
    }

    historySearchInput.addEventListener('input', renderHistoryTable);
    historyFilterType.addEventListener('change', renderHistoryTable);
    btnRefreshHistory.addEventListener('click', () => {
        loadHistory();
        showToast('History refreshed', 'info');
    });

    // =========================================================================
    // 12. Auth Modal & Forms
    // =========================================================================
    const authModal = document.getElementById('auth-modal');
    const modalCloseBtn = document.getElementById('modal-close-btn');
    const modalTabLogin = document.getElementById('modal-tab-login');
    const modalTabRegister = document.getElementById('modal-tab-register');
    const modalPanelLogin = document.getElementById('modal-panel-login');
    const modalPanelRegister = document.getElementById('modal-panel-register');

    const btnOpenLogin = document.getElementById('btn-open-login');
    const btnOpenRegister = document.getElementById('btn-open-register');
    const btnHistoryLoginTrigger = document.getElementById('btn-history-login-trigger');
    const btnHistoryRegisterTrigger = document.getElementById('btn-history-register-trigger');

    function openAuthModal(initialTab = 'login') {
        authModal.style.display = 'flex';
        if (initialTab === 'login') {
            modalTabLogin.classList.add('active');
            modalTabRegister.classList.remove('active');
            modalPanelLogin.style.display = 'block';
            modalPanelRegister.style.display = 'none';
        } else {
            modalTabRegister.classList.add('active');
            modalTabLogin.classList.remove('active');
            modalPanelRegister.style.display = 'block';
            modalPanelLogin.style.display = 'none';
        }
    }

    function closeAuthModal() {
        authModal.style.display = 'none';
    }

    btnOpenLogin.addEventListener('click', () => openAuthModal('login'));
    btnOpenRegister.addEventListener('click', () => openAuthModal('register'));
    btnHistoryLoginTrigger.addEventListener('click', () => openAuthModal('login'));
    btnHistoryRegisterTrigger.addEventListener('click', () => openAuthModal('register'));
    modalCloseBtn.addEventListener('click', closeAuthModal);

    authModal.addEventListener('click', (e) => {
        if (e.target === authModal) closeAuthModal();
    });

    modalTabLogin.addEventListener('click', () => {
        modalTabLogin.classList.add('active');
        modalTabRegister.classList.remove('active');
        modalPanelLogin.style.display = 'block';
        modalPanelRegister.style.display = 'none';
    });

    modalTabRegister.addEventListener('click', () => {
        modalTabRegister.classList.add('active');
        modalTabLogin.classList.remove('active');
        modalPanelRegister.style.display = 'block';
        modalPanelLogin.style.display = 'none';
    });

    // Form Submit: Login
    const formLogin = document.getElementById('form-login');
    const btnSubmitLogin = document.getElementById('btn-submit-login');
    formLogin.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('login-email').value.trim();
        const password = document.getElementById('login-password').value;

        btnSubmitLogin.disabled = true;
        btnSubmitLogin.textContent = 'Signing in...';

        try {
            const res = await fetch(`${API_BASE}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
            const data = await res.json();

            if (!res.ok || !data.success) {
                throw new Error(data.message || 'Invalid credentials');
            }

            state.token = data.data.token;
            state.user = {
                id: data.data.userId,
                name: data.data.name,
                email: data.data.email
            };
            localStorage.setItem('stegovault_token', state.token);
            localStorage.setItem('stegovault_user', JSON.stringify(state.user));

            updateAuthUi();
            closeAuthModal();
            formLogin.reset();
            showToast(`Welcome back, ${state.user.name}!`, 'success');

            loadHistory();
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btnSubmitLogin.disabled = false;
            btnSubmitLogin.textContent = 'Sign In';
        }
    });

    // Form Submit: Register
    const formRegister = document.getElementById('form-register');
    const btnSubmitRegister = document.getElementById('btn-submit-register');
    formRegister.addEventListener('submit', async (e) => {
        e.preventDefault();
        const name = document.getElementById('reg-name').value.trim();
        const email = document.getElementById('reg-email').value.trim();
        const password = document.getElementById('reg-password').value;
        const confirmPassword = document.getElementById('reg-confirm-password').value;

        if (password !== confirmPassword) {
            showToast('Password confirmation does not match', 'error');
            return;
        }

        btnSubmitRegister.disabled = true;
        btnSubmitRegister.textContent = 'Creating account...';

        try {
            const res = await fetch(`${API_BASE}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, email, password, confirmPassword })
            });
            const data = await res.json();

            if (!res.ok || !data.success) {
                throw new Error(data.message || 'Registration failed');
            }

            state.token = data.data.token;
            state.user = {
                id: data.data.userId,
                name: data.data.name,
                email: data.data.email
            };
            localStorage.setItem('stegovault_token', state.token);
            localStorage.setItem('stegovault_user', JSON.stringify(state.user));

            updateAuthUi();
            closeAuthModal();
            formRegister.reset();
            showToast(`Account created! Welcome, ${state.user.name}!`, 'success');

            loadHistory();
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            btnSubmitRegister.disabled = false;
            btnSubmitRegister.textContent = 'Create Account';
        }
    });
});
