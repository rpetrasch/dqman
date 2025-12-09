"""
Module for test / validate zip code data format
"""
import unittest
from zip_code_class import ZIPCode

class TestZIPCode(unittest.TestCase):

    def test_static_is_valid(self):
        """Tests the static is_valid() method with various inputs."""
        print("Testing static is_valid()")
        # Happy path - should be True
        self.assertTrue(ZIPCode.is_valid_zip_code("12345"))
        self.assertTrue(ZIPCode.is_valid_zip_code("90210"))
        self.assertTrue(ZIPCode.is_valid_zip_code("00000"))

        # Sad paths - should be False
        self.assertFalse(ZIPCode.is_valid_zip_code("1234"))      # Too short
        self.assertFalse(ZIPCode.is_valid_zip_code("123456"))    # Too long
        self.assertFalse(ZIPCode.is_valid_zip_code("1234a"))     # Contains letters
        self.assertFalse(ZIPCode.is_valid_zip_code("abcde"))     # All letters
        self.assertFalse(ZIPCode.is_valid_zip_code("12-34"))     # Contains symbols
        self.assertFalse(ZIPCode.is_valid_zip_code("123 45"))    # Contains spaces
        self.assertFalse(ZIPCode.is_valid_zip_code(""))          # Empty string
        self.assertFalse(ZIPCode.is_valid_zip_code(None))        # None input (will raise TypeError, regex match fails)


    def test_constructor(self):
        """Tests the object constructor __init__."""
        print("Testing constructor")
        # Happy path: Should create an object without errors
        try:
            zip_obj = ZIPCode("54321")
            self.assertIsInstance(zip_obj, ZIPCode)
        except ValueError:
            self.fail("ZIPCode constructor raised ValueError unexpectedly for a valid ZIP code.")

        # Sad path: Should raise ValueError for an invalid ZIP code
        with self.assertRaises(ValueError):
            ZIPCode("invalid")
        with self.assertRaises(ValueError):
            ZIPCode("123")


    def test_get_value(self):
        """Tests the get_value() method."""
        print("Testing get_value()")
        zip_str = "98765"
        zip_obj = ZIPCode(zip_str)
        self.assertEqual(zip_obj.get_value(), zip_str)


    def test_instance_is_valid(self):
        """Tests the instance-level is_valid() method."""
        print("Testing instance is_valid()")
        # We can only create valid objects, so this should always be true.
        zip_obj = ZIPCode("11223")
        self.assertTrue(zip_obj.is_valid())


    def test_equals(self):
        """Tests the equals() method."""
        print("Testing equals()")
        zip_obj = ZIPCode("10001")
        self.assertTrue(zip_obj.equals("10001"))
        self.assertFalse(zip_obj.equals("10002"))


    def test_region_match_not_implemented(self):
        """Tests that region_match() raises NotImplementedError."""
        print("Testing region_match()")
        zip_obj = ZIPCode("90210")
        with self.assertRaises(NotImplementedError):
            zip_obj.region_match("some_region")

    def test_equality_operator(self):
        """Tests the __eq__ method via the == operator."""
        print("Testing equality operator")
        zip1 = ZIPCode("12345")
        zip2 = ZIPCode("12345")
        zip3 = ZIPCode("54321")

        self.assertTrue(zip1 == zip2)
        self.assertFalse(zip1 == zip3)
        # Test comparison with a raw string
        self.assertTrue(zip1 == "12345")
        self.assertFalse(zip1 == "54321")

if __name__ == '__main__':
    unittest.main(verbosity=2)