(function () {
    'use strict';

    if (typeof window.profile !== 'object') {
        console.error('Blockly@rduino R4: board profile table is not available.');
        return;
    }

    var serialSpeeds = [
        ['300', '300'], ['600', '600'], ['1200', '1200'],
        ['2400', '2400'], ['4800', '4800'], ['9600', '9600'],
        ['14400', '14400'], ['19200', '19200'], ['28800', '28800'],
        ['31250', '31250'], ['38400', '38400'], ['57600', '57600'],
        ['115200', '115200']
    ];

    function unoR4Profile(description, uploadArg, helpLink, features) {
        return {
            description: description,
            cpu: 'Renesas RA4M1 (Arm Cortex-M4, 48 MHz)',
            speed: '115200',
            digital: ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13'],
            dropdownDigital: [
                ['0', '0'], ['1', '1'], ['2', '2'], ['3', '3'], ['4', '4'], ['5', '5'], ['6', '6'],
                ['7', '7'], ['8', '8'], ['9', '9'], ['10', '10'], ['11', '11'], ['12', '12'], ['13', '13']
            ],
            PWM: ['3', '5', '6', '9', '10', '11'],
            dropdownPWM: [['3', '3'], ['5', '5'], ['6', '6'], ['9', '9'], ['10', '10'], ['11', '11']],
            analog: ['A0', 'A1', 'A2', 'A3', 'A4', 'A5'],
            dropdownAnalog: [['A0', 'A0'], ['A1', 'A1'], ['A2', 'A2'], ['A3', 'A3'], ['A4', 'A4'], ['A5', 'A5']],
            I2C: ['A4', 'A5'],
            SPI: [['10 (SS)', '10'], ['11 (MOSI)', '11'], ['12 (MISO)', '12'], ['13 (SCK)', '13']],
            // Keep the conservative UNO-compatible interrupt list until the
            // interrupt block behaviour has been verified on real UNO R4 hardware.
            interrupt: ['2', '3'],

            // Temporary artwork fallback. Dedicated UNO R4 artwork will replace
            // these files once the R4 board profiles are merged into upstream UI.
            picture: 'media/boards/arduino_uno.jpg',
            miniPicture: 'media/boards/arduino_uno_mini.jpg',
            miniPicture_hor: 'media/boards/arduino_uno_mini_hor.jpg',

            serial: serialSpeeds,
            serialPin: [['0 (RX) ; 1 (TX)', '0']],
            upload_arg: uploadArg,
            help_link: helpLink,

            // R4-specific metadata. Legacy Blockly@rduino ignores unknown fields,
            // but we keep them here for future R4-specific blocks and UI.
            architecture: 'renesas_uno',
            dac: ['A0'],
            rtc: true,
            features: features
        };
    }

    window.profile.arduino_uno_r4_wifi = unoR4Profile(
        'Arduino UNO R4 WiFi',
        'arduino:renesas_uno:unor4wifi',
        'https://docs.arduino.cc/hardware/uno-r4-wifi/',
        {
            wifi: true,
            bluetooth: true,
            ledMatrix: '12x8',
            qwiic: true,
            can: true
        }
    );

    window.profile.arduino_uno_r4_minima = unoR4Profile(
        'Arduino UNO R4 Minima',
        'arduino:renesas_uno:minima',
        'https://docs.arduino.cc/hardware/uno-r4-minima/',
        {
            wifi: false,
            bluetooth: false,
            ledMatrix: false,
            qwiic: false,
            can: true
        }
    );

    function installR4BoardOptions() {
        var boardSelect = document.getElementById('board_select');
        if (!boardSelect) {
            return;
        }

        var arduinoGroup = boardSelect.querySelector('optgroup[label="Arduino"]');
        if (!arduinoGroup) {
            return;
        }

        function addBoardOption(value, label) {
            if (boardSelect.querySelector('option[value="' + value + '"]')) {
                return;
            }
            var option = document.createElement('option');
            option.value = value;
            option.textContent = label;
            arduinoGroup.insertBefore(option, arduinoGroup.firstChild);
        }

        // Insert Minima first so WiFi ends up at the very top.
        addBoardOption('arduino_uno_r4_minima', 'Arduino UNO R4 Minima');
        addBoardOption('arduino_uno_r4_wifi', 'Arduino UNO R4 WiFi');

        if (document.title.indexOf('UNO R4') === -1) {
            document.title += ' · UNO R4';
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', installR4BoardOptions);
    } else {
        installR4BoardOptions();
    }
})();
