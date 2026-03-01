"""
This module provides functions to save and load a list of countries. It also uses REST API to retrieve country names from the internet.
"""
import os
import pandas as pd
import requests

#
def request_countries() -> list[str]:
    """
    Request the countries from the REST API
    :return: country list
    """
    url = "https://restcountries.com/v3.1/all?fields=name"
    response = requests.get(url)
    data = response.json()
    # Extract the country names from the JSON response to get a list of country names
    country_list = [country["name"]["common"] for country in data]
    # countries_df = pd.DataFrame(countries, columns=["country"])  # We don't need a dataframe for now, but it could be helpful in the future
    return country_list


def save_countries(countries):
    """
    Save the countries to a file
    :param countries:
    :return: -
    """
    countries_df = pd.DataFrame(countries, columns=["country"])
    countries_df.to_csv("countries.csv", index=False)

#
def load_countries():
    """
     Load the countries from a file
    :param -
    :return: -
    """
    # read the countries from the file (mit first header line)
    countries_df = pd.read_csv("countries.csv")
    countries = countries_df["country"].tolist()
    return countries


def get_countries() -> list[str]:
    """
    Get the countries from a file (if it exists) or from the webservice
    :return: list of countries
    """
    # check whether the file exists
    if os.path.exists("countries.csv"):
        countries = load_countries()
    else:
        countries = request_countries()
        save_countries(countries)
    return countries
