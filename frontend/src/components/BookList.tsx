import React, { useState, useEffect } from 'react';

// Define structure of the book JSON
interface Book { 
    title: string;
    level: 'N5' | 'N4' | 'N3' | 'N2' | 'N1' | 'N/A';
    imageUrl: string;
}

const BookList: React.FC = () => { 
    const [books, setBooks] = useState<Book[]>([]);
    const [aniBooks, setAniBooks] = useState<Book[]>([]);
    const [ehonnaviBooks, setEhonnaviBooks] = useState<Book[]>([]);  // Separate state for Ehonnavi books
    const [filteredBooks, setFilteredBooks] = useState<Book[]>([]);
    const [filter, setFilter] = useState<string>('All');
    const [currentPage, setCurrentPage] = useState<number>(1);
    const booksPerPage = 10;

    // Colors for the buttons for different levels
    const levelColors: { [key: string]: string } = {
        N5: 'bg-blue-500 text-white',
        N4: 'bg-green-500 text-white',
        N3: 'bg-yellow-500 text-white',
        N2: 'bg-orange-500 text-white',
        N1: 'bg-red-500 text-white',
    };

    const levelHoverColors: { [key: string]: string } = {
        N5: 'hover:bg-blue-500 hover:text-white duration-300 transition ease-in-out delay-50 hover:-translate-y-1',
        N4: 'hover:bg-green-500 hover:text-white duration-300 transition ease-in-out delay-50 hover:-translate-y-1',
        N3: 'hover:bg-yellow-500 hover:text-white duration-300 transition ease-in-out delay-50 hover:-translate-y-1',
        N2: 'hover:bg-orange-500 hover:text-white duration-300 transition ease-in-out delay-50 hover:-translate-y-1',
        N1: 'hover:bg-red-700 hover:text-white duration-300 transition ease-in-out delay-50 hover:-translate-y-1',
    };

    useEffect(() => {
        // Fetch JSON data from honto file
        fetch('http://localhost:8080/api/books')
            .then(response => response.json())
            .then(data => {
                const allBooks = Object.keys(data).flatMap(level =>
                    data[level].map((book: { title: string, imageUrl: string }) => ({ 
                        title: book.title,
                        level,
                        imageUrl: book.imageUrl,
                    }))
                    .filter((book : Book) => book.level !== 'N/A')
                );
                setBooks(allBooks);
                setFilteredBooks(allBooks);  // Initialize filteredBooks with allBooks
            })
            .catch(error => console.error("Error loading the book data: ", error));

        // Fetch JSON data from ehonnavi file (N5 level books)
        fetch('http://localhost:8080/api/ehonnavi-books')
            .then(response => response.json())
            .then(data => {
                const ehonnaviBooks = data.N5.map((book: { title: string, imageUrl: string }) => ({
                    title: book.title,
                    imageUrl: book.imageUrl,
                    level: 'N5' 
                }));

                setEhonnaviBooks(ehonnaviBooks);  // Store separately for now
            })
            .catch(error => console.error("Error loading the Ehonnavi books data: ", error));

        // Fetch JSON data from anionline file
        fetch('http://localhost:8080/api/ani-books')
            .then(response => response.json())
            .then(data => {
                const allAniBooks = Object.keys(data).flatMap(level =>
                    data[level].map((book: { title: string, imageUrl: string }) => ({ 
                        title: book.title, 
                        imageUrl: book.imageUrl, 
                        level 
                    }))
                );
                setAniBooks(allAniBooks);
            })
            .catch(error => console.error("Error loading the Ani books data: ", error));
    }, []);


    // Combine all books and apply the filter
    useEffect(() => {
        const combinedBooks = [...books, ...ehonnaviBooks, ...aniBooks];  // Merge all book sources
        const allFilteredBooks = filter === 'All' 
            ? combinedBooks 
            : combinedBooks.filter(book => book.level === filter);

        setFilteredBooks(allFilteredBooks);  // Update filtered books based on filter
        setCurrentPage(1);  // Reset to first page when filter changes
    }, [filter, books, aniBooks, ehonnaviBooks]);

    // Calculate total pages
    const totalPages = Math.ceil(filteredBooks.length / booksPerPage);

    // Slice books to only show books for the current page
    const currentBooks = filteredBooks.slice(
        (currentPage - 1) * booksPerPage,
        currentPage * booksPerPage
    );

    // Pagination controls
    const paginate = (pageNumber: number) => setCurrentPage(pageNumber);

    const renderPagination = () => {
        const pageNumbers = [];
    
        if (totalPages <= 4) {
            // Show all pages if there are 4 or fewer
            for (let i = 1; i <= totalPages; i++) {
                pageNumbers.push(i);
            }
        } else {
            // Always show the first 3 pages
            pageNumbers.push(1, 2, 3);
    
            if (currentPage > 4) {
                // Add `...` if the current page is beyond page 4
                pageNumbers.push('...');
            }
    
            // Add the current page if it's between 4 and the last page - 1
            if (currentPage > 3 && currentPage < totalPages) {
                pageNumbers.push(currentPage);
            }
    
            if (currentPage < totalPages - 1) {
                // Add `...` if there are more pages after the current one and before the last page
                pageNumbers.push('...');
            }
    
            // Always show the last page
            pageNumbers.push(totalPages);
        }
    
        // Render the page buttons
        return pageNumbers.map((page, index) =>
            typeof page === 'number' ? (
                <button
                    key={index}
                    className={`px-4 py-2 border mx-1 ${
                        currentPage === page ? 'bg-slate-50 text-black' : 'bg-black text-white hover:bg-slate-50 hover:text-black duration-400 transition ease-in-out delay-50'
                    }`}
                    onClick={() => paginate(page)}
                >
                    {page}
                </button>
            ) : (
                <span key={index} className="px-4 py-2 border mx-1 text-black">
                    {page}
                </span>
            )
        );
    };
    return (
        <div className='container mx-auto p-4'>
            <div className='mb-4 flex space-x-2 justify-center'>
                {['N5', 'N4', 'N3', 'N2', 'N1'].map((level) => (
                    <button
                        key={level}
                        className={`p-2 border ${
                            filter === level ? levelColors[level] : 'bg-black text-white'
                        } ${levelHoverColors[level]}`}
                        onClick={() => setFilter(level)}
                    >
                        {level}
                    </button>
                ))}
            </div>

            <ul className='list-none pl-5 text-slate-100'>
                {currentBooks.map((book, index) => (
                    <li key={index}>
                        <div className='flex items-center space-x-4 justify-center'>
                            <img src={book.imageUrl} alt={book.title} className='w-24 h-24 object-cover mb-2'/>
                        </div>
                        <div className='mb-4'>
                            {book.title} ({book.level})
                        </div>
                    </li>
                ))}
            </ul>

            {/* Pagination controls */}
            <div className='flex justify-center mt-6'>
                <button
                    className='px-4 py-2 border mr-2 text-white bg-black hover:bg-slate-50 hover:text-black duration-400 transition ease-in-out delay-50'
                    onClick={() => paginate(Math.max(currentPage - 1, 1))}
                    disabled={currentPage === 1}
                >
                    Previous
                </button>

                {renderPagination()}

                <button
                    className='px-4 py-2 border ml-2 text-white bg-black hover:bg-slate-50 hover:text-black duration-400 transition ease-in-out delay-50'
                    onClick={() => paginate(Math.min(currentPage + 1, totalPages))}
                    disabled={currentPage === totalPages}
                >
                    Next
                </button>
            </div>
        </div>
    );
};
export default BookList;
