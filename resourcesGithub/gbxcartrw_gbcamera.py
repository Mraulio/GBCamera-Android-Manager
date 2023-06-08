# -*- coding: utf-8 -*-
# Author: Lesserkuma (github.com/lesserkuma)

import time, math, struct, traceback, zlib, copy, hashlib, os, datetime, platform
import serial, serial.tools.list_ports
from serial import SerialException

class GbxDevice:
	DEVICE_CMD = {
		"NULL":0x30,
		"OFW_RESET_AVR":0x2A,
		"OFW_CART_MODE":0x43,
		"OFW_FW_VER":0x56,
		"OFW_PCB_VER":0x68,
		"OFW_USART_1_7M_SPEED":0x3E,
		"OFW_CART_PWR_ON":0x2F,
		"OFW_CART_PWR_OFF":0x2E,
		"OFW_QUERY_CART_PWR":0x5D,
		"OFW_DONE_LED_ON":0x3D,
		"OFW_ERROR_LED_ON":0x3F,
		"OFW_GB_CART_MODE":0x47,
		"OFW_GB_FLASH_BANK_1_COMMAND_WRITES":0x4E,
		"OFW_LNL_QUERY":0x25,
		"QUERY_FW_INFO":0xA1,
		"SET_MODE_AGB":0xA2,
		"SET_MODE_DMG":0xA3,
		"SET_VOLTAGE_3_3V":0xA4,
		"SET_VOLTAGE_5V":0xA5,
		"SET_VARIABLE":0xA6,
		"SET_FLASH_CMD":0xA7,
		"SET_ADDR_AS_INPUTS":0xA8,
		"CLK_HIGH":0xA9,
		"CLK_LOW":0xAA,
		"DMG_CART_READ":0xB1,
		"DMG_CART_WRITE":0xB2,
		"DMG_CART_WRITE_SRAM":0xB3,
		"DMG_MBC_RESET":0xB4,
		"DMG_MBC7_READ_EEPROM":0xB5,
		"DMG_MBC7_WRITE_EEPROM":0xB6,
		"DMG_MBC6_MMSA_WRITE_FLASH":0xB7,
		"DMG_SET_BANK_CHANGE_CMD":0xB8,
		"DMG_EEPROM_WRITE":0xB9,
		"AGB_CART_READ":0xC1,
		"AGB_CART_WRITE":0xC2,
		"AGB_CART_READ_SRAM":0xC3,
		"AGB_CART_WRITE_SRAM":0xC4,
		"AGB_CART_READ_EEPROM":0xC5,
		"AGB_CART_WRITE_EEPROM":0xC6,
		"AGB_CART_WRITE_FLASH_DATA":0xC7,
		"AGB_CART_READ_3D_MEMORY":0xC8,
		"AGB_BOOTUP_SEQUENCE":0xC9,
		"DMG_FLASH_WRITE_BYTE":0xD1,
		"AGB_FLASH_WRITE_SHORT":0xD2,
		"FLASH_PROGRAM":0xD3,
		"CART_WRITE_FLASH_CMD":0xD4,
	}
	# \#define VAR(\d+)_([^\t]+)\t+(.+)
	DEVICE_VAR = {
		"ADDRESS":[32, 0x00],
		"TRANSFER_SIZE":[16, 0x00],
		"BUFFER_SIZE":[16, 0x01],
		"DMG_ROM_BANK":[16, 0x02],
		"CART_MODE":[8, 0x00],
		"DMG_ACCESS_MODE":[8, 0x01],
		"FLASH_COMMAND_SET":[8, 0x02],
		"FLASH_METHOD":[8, 0x03],
		"FLASH_WE_PIN":[8, 0x04],
		"FLASH_PULSE_RESET":[8, 0x05],
		"FLASH_COMMANDS_BANK_1":[8, 0x06],
		"FLASH_SHARP_VERIFY_SR":[8, 0x07],
		"DMG_READ_CS_PULSE":[8, 0x08],
		"DMG_WRITE_CS_PULSE":[8, 0x09],
		"FLASH_DOUBLE_DIE":[8, 0x0A],
		"DMG_READ_METHOD":[8, 0x0B],
		"AGB_READ_METHOD":[8, 0x0C],
	}

	PCB_VERSIONS = {4:'v1.3', 5:'v1.4', 6:'v1.4a', 101:'Mini v1.0d'}
	
	FW = []
	FW_UPDATE_REQ = False
	FW_VAR = {}
	MODE = None
	PORT = ''
	DEVICE = None
	BAUDRATE = 1000000

	def __init__(self, port=None):
		if port is None:
			print("No port specified.")
			return
		try:
			dev = serial.Serial(port, self.BAUDRATE, timeout=0.1)
			self.DEVICE = dev
		
		except SerialException as e:
			if "Permission" in str(e):
				print("The GBxCart RW device on port " + port + " couldn’t be accessed. Make sure your user account has permission to use it and it’s not already in use by another application.")
				return
			else:
				print("A critical error occured while trying to access the GBxCart RW device on port " + port + ".\n\n" + str(e))
				return
		
		print(f"Connected to GBxCart RW at port {port}.")

	def __del__(self):
		try:
			self.CartPowerOff(delay=0)
			self.DEVICE.Close()
		except:
			pass
	
	def wait_for_ack(self, values=None):
		if values is None: values = [0x01, 0x03]
		buffer = self._read(1)
		if buffer not in values:
			tb_stack = traceback.extract_stack()
			stack = tb_stack[len(tb_stack)-2] # caller only
			if stack.name == "_write": stack = tb_stack[len(tb_stack)-3]
			if buffer is False:
				print("Timeout error ({:s}(), line {:d})".format(stack.name, stack.lineno))
			else:
				print("Communication error ({:s}(), line {:d})".format(stack.name, stack.lineno))
			self.ERROR = True
			self.CANCEL = True
			return False
		
		return buffer

	def _write(self, data, wait=False):
		if not isinstance(data, bytearray):
			data = bytearray([data])
		
		cmds = dict((v,k) for k,v in self.DEVICE_CMD.items())
		dstr = ' '.join(format(x, '02X') for x in data)
		print("[{:02X}] [{:s}] {:s}".format(int(len(dstr)/3) + 1, cmds[data[0]], dstr[:96]))
		
		self.DEVICE.write(data)
		self.DEVICE.flush()
		
		# On MacOS it’s possible not all bytes are transmitted successfully,
		# even though we’re using flush() which is the tcdrain function.
		# Still looking for a better solution than delaying here.
		if platform.system() == "Darwin":
			time.sleep(0.00125)
		
		if wait: return self.wait_for_ack()
	
	def _read(self, count):
		buffer = self.DEVICE.read(count)
		if len(buffer) != count:
			print("Error: Received {:d} byte(s) instead of the expected {:d} byte(s)".format(len(buffer), count))
			while self.DEVICE.in_waiting > 0:
				self.DEVICE.reset_input_buffer()
				time.sleep(0.5)
			self.DEVICE.reset_output_buffer()
			return False
		
		if count == 1:
			return buffer[0]
		else:
			return bytearray(buffer)

	def _set_fw_variable(self, key, value):
		print("Setting firmware variable {:s} to 0x{:X}".format(key, value))
		self.FW_VAR[key] = value

		size = 0
		for (k, v) in self.DEVICE_VAR.items():
			if key in k:
				if v[0] == 8: size = 1
				elif v[0] == 16: size = 2
				elif v[0] == 32: size = 4
				key = v[1]
				break
		if size == 0:
			raise Exception("Unknown variable name specified.")
		
		buffer = bytearray([self.DEVICE_CMD["SET_VARIABLE"], size])
		buffer.extend(struct.pack(">I", key))
		buffer.extend(struct.pack(">I", value))
		self._write(buffer)
	
	def CartPowerCycle(self, delay=0.1):
		if self.CanPowerCycleCart():
			self.CartPowerOff(delay=delay)
			self.CartPowerOn(delay=delay)
			if self.MODE == "DMG":
				self._write(self.DEVICE_CMD["SET_MODE_DMG"])
			elif self.MODE == "AGB":
				self._write(self.DEVICE_CMD["SET_MODE_AGB"])

	def CartPowerOff(self, delay=0.1):
		print("Turning off power to the cartridge")
		self._write(self.DEVICE_CMD["OFW_CART_PWR_OFF"])
		time.sleep(delay)
	
	def CartPowerOn(self, delay=0.1):
		print("Checking if cartridge power is on")
		self._write(self.DEVICE_CMD["OFW_QUERY_CART_PWR"])
		if self._read(1) == 0:
			print("Turning on power to the cartridge")
			self._write(self.DEVICE_CMD["OFW_CART_PWR_ON"])
			time.sleep(delay)
			self.DEVICE.reset_input_buffer() # bug workaround

	def SetCartType(self, mode):
		if mode == "DMG":
			print("Setting mode to DMG")
			self._write(self.DEVICE_CMD["SET_MODE_DMG"])
			print("Setting voltage to 5V")
			self._write(self.DEVICE_CMD["SET_VOLTAGE_5V"])
			self._set_fw_variable("DMG_READ_METHOD", 1)
			self._set_fw_variable("CART_MODE", 1)
			self.MODE = "DMG"
		elif mode == "AGB":
			print("Setting mode to AGB")
			self._write(self.DEVICE_CMD["SET_MODE_AGB"])
			print("Setting voltage to 3.3V")
			self._write(self.DEVICE_CMD["SET_VOLTAGE_3_3V"])
			self._set_fw_variable("AGB_READ_METHOD", 0)
			self._set_fw_variable("CART_MODE", 2)
			self.MODE = "AGB"
		self._set_fw_variable(key="ADDRESS", value=0)
		#self.CartPowerOn()

	def CartRead_ROM(self, address, length, max_length=64):
		num = math.ceil(length / max_length)
		print("Reading 0x{:X} bytes from ROM at 0x{:X} in {:d} iteration(s)".format(length, address, num))
		if length > max_length: length = max_length
		
		buffer = bytearray()
		self._set_fw_variable("TRANSFER_SIZE", length)
		if self.MODE == "DMG":
			self._set_fw_variable("ADDRESS", address)
			self._set_fw_variable("DMG_ACCESS_MODE", 1) # MODE_ROM_READ
		elif self.MODE == "AGB":
			self._set_fw_variable("ADDRESS", address >> 1)
		
		if self.MODE == "DMG":
			command = "DMG_CART_READ"
		elif self.MODE == "AGB":
			command = "AGB_CART_READ"
		
		for _ in range(0, num):
			self._write(self.DEVICE_CMD[command])
			temp = self._read(length)
			if isinstance(temp, int): temp = bytearray([temp])
			if temp is False or len(temp) != length: return bytearray()
			buffer += temp
		
		return buffer

	def CartRead_SRAM(self, address, length, max_length=64):
		num = math.ceil(length / max_length)
		print("Reading 0x{:X} bytes from cartridge RAM in {:d} iteration(s)".format(length, num))
		if length > max_length: length = max_length
		buffer = bytearray()
		self._set_fw_variable("TRANSFER_SIZE", length)
		
		if self.MODE == "DMG":
			self._set_fw_variable("ADDRESS", 0xA000 + address)
			self._set_fw_variable("DMG_ACCESS_MODE", 3) # MODE_RAM_READ
			self._set_fw_variable("DMG_READ_CS_PULSE", 1)
			command = self.DEVICE_CMD["DMG_CART_READ"]
		elif self.MODE == "AGB":
			self._set_fw_variable("ADDRESS", address)
			command = self.DEVICE_CMD["AGB_CART_READ_SRAM"]
		
		for _ in range(0, num):
			self._write(command)
			temp = self._read(length)
			if isinstance(temp, int): temp = bytearray([temp])
			if temp is False or len(temp) != length: return bytearray()
			buffer += temp
		
		return buffer

	def _cart_write_sram(self, address, buffer, command=None):
		length = len(buffer)
		max_length = 256
		num = math.ceil(length / max_length)
		print("Write 0x{:X} bytes to cartridge RAM in {:d} iteration(s)".format(length, num))
		if length > max_length: length = max_length

		self._set_fw_variable("TRANSFER_SIZE", length)
		if self.MODE == "DMG":
			self._set_fw_variable("ADDRESS", 0xA000 + address)
			self._set_fw_variable("DMG_ACCESS_MODE", 4) # MODE_RAM_WRITE
			self._set_fw_variable("DMG_WRITE_CS_PULSE", 1)
			if command is None: command = self.DEVICE_CMD["DMG_CART_WRITE_SRAM"]
		elif self.MODE == "AGB":
			self._set_fw_variable("ADDRESS", address)
			if command is None: command = self.DEVICE_CMD["AGB_CART_WRITE_SRAM"]

		for i in range(0, num):
			self._write(command)
			self._write(buffer[i*length:i*length+length])
			self._read(1)
		
		if self.MODE == "DMG":
			self._set_fw_variable("ADDRESS", 0)
			self._set_fw_variable("DMG_WRITE_CS_PULSE", 0)
		
		return True

	def _cart_write(self, address, value, flashcart=False):
		print("Writing to cartridge: 0x{:X} = 0x{:X}".format(address, value & 0xFF))
		if self.MODE == "DMG":
			if flashcart:
				buffer = bytearray([self.DEVICE_CMD["DMG_FLASH_WRITE_BYTE"]])
			else:
				buffer = bytearray([self.DEVICE_CMD["DMG_CART_WRITE"]])
			buffer.extend(struct.pack(">I", address))
			buffer.extend(struct.pack("B", value & 0xFF))
		elif self.MODE == "AGB":
			if flashcart:
				buffer = bytearray([self.DEVICE_CMD["AGB_FLASH_WRITE_SHORT"]])
			else:
				buffer = bytearray([self.DEVICE_CMD["AGB_CART_WRITE"]])
			
			buffer.extend(struct.pack(">I", address >> 1))
			buffer.extend(struct.pack(">H", value & 0xFFFF))
		self._write(buffer)
	
	def CartWrite_ROM(self, address, value):
		return self._cart_write(address=address, value=value)

	def CartWrite_SRAM(self, address, buffer):
		return self._cart_write_sram(address=address, buffer=bytearray(buffer))
	
	def AgbBootupSequence(self):
		self._write(self.DEVICE_CMD["AGB_BOOTUP_SEQUENCE"], wait=True)
		

#######

gbxcart = None
port = None
comports = serial.tools.list_ports.comports()
for i in range(0, len(comports)):
	if comports[i].vid == 0x1A86 and comports[i].pid == 0x7523:
		port = comports[i].device
		break

gbxcart = GbxDevice(port=port)
gbxcart.CartPowerOff()
gbxcart.SetCartType("DMG")
gbxcart.CartPowerOn()
# Read ROM title
print("ROM title: {:s}".format(gbxcart.CartRead_ROM(address=0x134, length=0x10).decode("ASCII", "ignore")))

# Enable SRAM access
gbxcart.CartWrite_ROM(address=0x6000, value=0x01)
gbxcart.CartWrite_ROM(address=0x0000, value=0x0A)

# Loop over 16 SRAM banks
savedata = bytearray()
for i in range(0, 16):
	print(f"Reading SRAM bank {i:d}")
	# Set SRAM bank
	gbxcart.CartWrite_ROM(address=0x4000, value=i)
	# Read 8 KiB of SRAM
	savedata += gbxcart.CartRead_SRAM(address=0, length=0x2000)

# Disable SRAM access
gbxcart.CartWrite_ROM(address=0x0000, value=0x00)
gbxcart.CartWrite_ROM(address=0x6000, value=0x00)
gbxcart.CartPowerOff()
del(gbxcart)

with open("savedata.sav", "wb") as f: f.write(savedata)
