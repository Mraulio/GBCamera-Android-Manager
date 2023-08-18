package com.mraulio.gbcameramanager.gbxcart;

import java.util.HashMap;
import java.util.Map;

public class GBxCartConstants {
    public static final int BAUDRATE = 1000000;

    public static HashMap<String, Integer> DEVICE_CMD = new HashMap<String, Integer>() {{
        put("NULL", 0x30);
        put("OFW_RESET_AVR", 0x2A);
        put("OFW_CART_MODE", 0x43);
        put("OFW_FW_VER", 0x56);
        put("OFW_PCB_VER", 0x68);
        put("OFW_USART_1_7M_SPEED", 0x3E);
        put("OFW_CART_PWR_ON", 0x2F);
        put("OFW_CART_PWR_OFF", 0x2E);
        put("OFW_QUERY_CART_PWR", 0x5D);
        put("OFW_DONE_LED_ON", 0x3D);
        put("OFW_ERROR_LED_ON", 0x3F);
        put("OFW_GB_CART_MODE", 0x47);
        put("OFW_GB_FLASH_BANK_1_COMMAND_WRITES", 0x4E);
        put("OFW_LNL_QUERY", 0x25);
        put("QUERY_FW_INFO", 0xA1);
        put("SET_MODE_AGB", 0xA2);
        put("SET_MODE_DMG", 0xA3);
        put("SET_VOLTAGE_3_3V", 0xA4);
        put("SET_VOLTAGE_5V", 0xA5);
        put("SET_VARIABLE", 0xA6);
        put("SET_FLASH_CMD", 0xA7);
        put("SET_ADDR_AS_INPUTS", 0xA8);
        put("CLK_HIGH", 0xA9);
        put("CLK_LOW", 0xAA);
        put("DMG_CART_READ", 0xB1);
        put("DMG_CART_WRITE", 0xB2);
        put("DMG_CART_WRITE_SRAM", 0xB3);
        put("DMG_MBC_RESET", 0xB4);
        put("DMG_MBC7_READ_EEPROM", 0xB5);
        put("DMG_MBC7_WRITE_EEPROM", 0xB6);
        put("DMG_MBC6_MMSA_WRITE_FLASH", 0xB7);
        put("DMG_SET_BANK_CHANGE_CMD", 0xB8);
        put("DMG_EEPROM_WRITE", 0xB9);
        put("AGB_CART_READ", 0xC1);
        put("AGB_CART_WRITE", 0xC2);
        put("AGB_CART_READ_SRAM", 0xC3);
        put("AGB_CART_WRITE_SRAM", 0xC4);
        put("AGB_CART_READ_EEPROM", 0xC5);
        put("AGB_CART_WRITE_EEPROM", 0xC6);
        put("AGB_CART_WRITE_FLASH_DATA", 0xC7);
        put("AGB_CART_READ_3D_MEMORY", 0xC8);
        put("AGB_BOOTUP_SEQUENCE", 0xC9);
        put("DMG_FLASH_WRITE_BYTE", 0xD1);
        put("AGB_FLASH_WRITE_SHORT", 0xD2);
        put("FLASH_PROGRAM", 0xD3);
        put("CART_WRITE_FLASH_CMD", 0xD4);
    }};
    public static Map<String, int[]> DEVICE_VAR = new HashMap<String, int[]>() {{
        put("ADDRESS", new int[] {32, 0x00});
        put("TRANSFER_SIZE", new int[] {16, 0x00});
        put("BUFFER_SIZE", new int[] {16, 0x01});
        put("DMG_ROM_BANK", new int[] {16, 0x02});
        put("CART_MODE", new int[] {8, 0x00});
        put("DMG_ACCESS_MODE", new int[] {8, 0x01});
        put("FLASH_COMMAND_SET", new int[] {8, 0x02});
        put("FLASH_METHOD", new int[] {8, 0x03});
        put("FLASH_WE_PIN", new int[] {8, 0x04});
        put("FLASH_PULSE_RESET", new int[] {8, 0x05});
        put("FLASH_COMMANDS_BANK_1", new int[] {8, 0x06});
        put("FLASH_SHARP_VERIFY_SR", new int[] {8, 0x07});
        put("DMG_READ_CS_PULSE", new int[] {8, 0x08});
        put("DMG_WRITE_CS_PULSE", new int[] {8, 0x09});
        put("FLASH_DOUBLE_DIE", new int[] {8, 0x0A});
        put("DMG_READ_METHOD", new int[] {8, 0x0B});
        put("AGB_READ_METHOD", new int[] {8, 0x0C});
    }};

    public Map<Integer, String> PCB_VERSIONS = new HashMap<Integer, String>() {{
        put(4, "v1.3");
        put(5, "v1.4");
        put(6, "v1.4a");
        put(101, "Mini v1.0d");
    }};


}
