import os

from flask import Flask, request, jsonify
import requests
from bs4 import BeautifulSoup
import urllib.parse

from scraper_error_code import ScraperErrorCode

app = Flask(__name__)


@app.route('/find_price', methods=['POST'])
def find_price_endpoint():
    data = request.get_json()
    if not data or 'productName' not in data:
        return make_error_response(ScraperErrorCode.MISSING_PARAM, "Missing parameter 'productName'", 400)

    product_name = data['productName']
    product_url = find_best_product_link(product_name)

    if not product_url:
        return make_error_response(ScraperErrorCode.PRODUCT_NOT_FOUND, "Could not find item", 404)

    title, price = extract_cheapest_offer(product_url)

    result =  jsonify({
        "found_product_name": title,
        "price": price,
        "currency": "PLN",
        "ceneo_url": product_url
    }), 200

    print(result)
    return result

def find_best_product_link(product_name):
    encoded_name = urllib.parse.quote(product_name)
    search_url = f"https://www.ceneo.pl/;szukaj-{encoded_name}"

    print(f"Searching: {search_url}")
    soup = get_soup(search_url)
    if not soup:
        return None

    first_product = soup.find("div", class_="cat-prod-row")

    if not first_product:
        if soup.find("table", class_="product-offers"):
            return search_url
        return None

    link_tag = first_product.find("a", class_="js_seoUrl")
    if not link_tag:
        link_tag = first_product.find("a", class_="go-to-product")
    if not link_tag:
        name_cont = first_product.find("strong", class_="cat-prod-row__name")
        if name_cont:
            link_tag = name_cont.find("a")

    if link_tag and 'href' in link_tag.attrs:
        return "https://www.ceneo.pl" + link_tag['href']

    return None

def get_soup(url):
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
        'Accept-Language': 'pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7'
    }
    try:
        response = requests.get(url, headers=headers, timeout=10)
        if response.status_code == 200:
            return BeautifulSoup(response.content, 'html.parser')
    except Exception as e:
        print(f"Connection error: {e}")
    return None

@app.route('/scrape_direct_url', methods=['POST'])
def scrape_direct_url_endpoint():
    data = request.get_json()

    if not data or 'url' not in data:
        return make_error_response(ScraperErrorCode.MISSING_PARAM, "Missing parameter 'url'", 400)
    url = data['url']

    parsed_url = urllib.parse.urlparse(url)
    domain = parsed_url.hostname

    if not domain:
        temp_url = "https://" + url
        parsed_url = urllib.parse.urlparse(temp_url)
        domain = parsed_url.hostname
        url = temp_url

    is_valid_domain = domain and (domain == "ceneo.pl" or domain.endswith(".ceneo.pl"))

    if not is_valid_domain:
        return make_error_response(ScraperErrorCode.INVALID_DOMAIN, "Invalid link not from ceneo", 400)

    title, price = extract_cheapest_offer(url)

    if price == 0.0:
        return make_error_response(ScraperErrorCode.PRICE_PARSING_ERROR, "Invalid price", 422)

    result =  jsonify({
        "found_product_name": title,
        "price": price,
        "currency": "PLN",
        "ceneo_url": url
    }), 200

    print(result)
    return result

def extract_cheapest_offer(product_url):
    print(f"Scraping product page: {product_url}")
    soup = get_soup(product_url)
    if not soup:
        return None, 0.0

    title_tag = soup.find("h1", class_="product-top__product-info__name")
    product_title = title_tag.get_text().strip() if title_tag else "Unknown product"

    offers = soup.find_all("div", class_="product-offer__container")

    best_price = float('inf')
    found = False

    for offer in offers:
        try:
            price_tag = offer.find("span", class_="price")
            if not price_tag: continue

            value_span = price_tag.find("span", class_="value")
            penny_span = price_tag.find("span", class_="penny")

            if value_span:
                full_price_str = value_span.get_text().strip().replace(" ", "")
                if penny_span:
                    full_price_str += penny_span.get_text().strip().replace(",", ".")
                else:
                    full_price_str = full_price_str.replace(",", ".")

                current_price = float(full_price_str)

                if current_price < best_price:
                    best_price = current_price
                    found = True
        except Exception:
            continue

    if found:
        return product_title, best_price

    return product_title, 0.0

def make_error_response(error_enum: ScraperErrorCode, message: str, status_code: int):
    return jsonify({
        "errorCode": error_enum.value,
        "message": message
    }), status_code




if __name__ == '__main__':
    app.run(port=5000, debug=True)


    # port = int(os.environ.get("PORT", 8080))
    # app.run(host="0.0.0.0", port=port)