"""
Module for ZIP code data definition as a class
"""
import re

class ZIPCode:
    """
    Class for ZIP Code
    Attributes:
        zip_code (str): The ZIP code value.
    """

    def __init__(self, zip_code: str):
        """
        Constructor
        Check whether a given ZIP code is valid before the object is created
        :param zip_code: ZIP code value
        :raise ValueError when the given ZIP code is not valid
        """
        # Should we prevent the creation of invalid zip codes? If yes, then this would do it:
        if not ZIPCode.is_valid_zip_code(zip_code):
            raise ValueError("Invalid zip code format")
        self.zip_code = zip_code

    def __str__(self) -> str:
        """String representation for end-users (e.g., print())."""
        return self.zip_code

    def __repr__(self) -> str:
        """Unambiguous representation for developers."""
        return f"ZIPCode('{self.zip_code}')"

    def __eq__(self, other) -> bool:
        """
        Compares two ZIP code objects for equality using the '==' operator.
        Can also compare to a string.
        """
        if isinstance(other, ZIPCode):
            return self.zip_code == other.zip_code
        if isinstance(other, str):
            return self.zip_code == other
        return NotImplemented

    def is_valid(self) -> bool:
        """
        Function for zip code validation (instance method)
        """
        return ZIPCode.is_valid_zip_code(self.zip_code) # Delegate to static method

    def get_value(self) -> str:
        """Returns the ZIP code as a string. Not necessary because __str__ exists"""
        return self.zip_code

    def equals(self, zip_code:str) -> bool:
        """Compares two ZIP code objects for equality. This method is redundant because __eq__ exists"""
        return zip_code == self.zip_code

    def region_match(self, region: str) -> bool:
        """
        Checks if the ZIP code belongs to a specific region based on a prefix or pattern.
        Since this is very specific for countries, we need to narrow it down, e.g. the first digit of a U.S. ZIP code
        indicates one of 10 large geographical areas, moving from the Northeast (0) to the West Coast (9).
        This provides a general location.
        See https://en.wikipedia.org/wiki/ZIP_Code
        :param region: name of the region as a string
        :return: yes or no
        """
        raise NotImplementedError("Not yet implemented")  # Exercise: Can you implement this?

    @staticmethod
    def is_valid_zip_code(zip_code: str) -> bool:
        """
        Function for zip code validation (static method)
        """
         # Validates zip code format: 5 digits (e. g., 12345)
        pattern = r'^\d{5}$'
        return re.match(pattern, zip_code) is not None