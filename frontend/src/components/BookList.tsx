import React, { useState, useEffect } from 'react';

// Define structure of the book JSON
interface Book { 
    title: string;
    level: 'N5' | 'N4' | 'N3' | 'N2' | 'N1';
    imageUrl: string;
}

const BookList: React.FC = () => { 
    const [books, setBooks] = useState<Book[]>([]);
    const [aniBooks, setAniBooks] = useState<Book[]>([]);
    const [filteredBooks, setFilteredBooks] = useState<Book[]>([]);
    const [filter, setFilter] = useState<string>('All');

    // Colors for the buttons for different levels
    const levelHoverColors: { [key: string]: string } = {
        N5: 'hover:bg-blue-500 duration-300 hover:text-white transition ease-in-out delay-50 hover:-translate-y-1',
        N4: 'hover:bg-green-500 duration-300 hover:text-white transition ease-in-out delay-50 hover:-translate-y-1',
        N3: 'hover:bg-yellow-500 duration-300 hover:text-white transition ease-in-out delay-50 hover:-translate-y-1',
        N2: 'hover:bg-orange-500 duration-300 hover:text-white transition ease-in-out delay-50 hover:-translate-y-1',
        N1: 'hover:bg-red-500 duration-300 hover:text-white transition ease-in-out delay-50 hover:-translate-y-1',
    };

    useEffect(() => {
        // Fetch JSON data from honto file
        fetch('http://localhost:8080/api/books')
            .then(response => response.json())
            .then(data => {
                const allBooks = Object.keys(data).flatMap(level =>
                    data[level].map((title: string) => ({ title, level }))
                );
                setBooks(allBooks);
                setFilteredBooks(allBooks);
            })
            .catch(error => console.error("Error loading the book data: ", error));

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

    // Update the filtered books whenever the filter changes
    useEffect(() => {
        const allFilteredBooks = books.concat(aniBooks);
        setFilteredBooks(
            filter === 'All' ? allFilteredBooks : allFilteredBooks.filter(book => book.level === filter)
        );
    }, [filter, books, aniBooks]);

    return (
        <div className='container mx-auto p-4'>
            <div className='mb-4 flex space-x-2 justify-center'>
                {['N5', 'N4', 'N3', 'N2', 'N1'].map((level) => (
                    <button
                        key={level}
                        className={`p-2 border bg-white text-black ${levelHoverColors[level]}`}
                        onClick={() => setFilter(level)}
                    >
                        {level}
                    </button>
                ))}
            </div>
            <ul className='list-none pl-5 text-slate-100'>
                {filteredBooks.map((book, index) => (
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
        </div>
    );
};

export default BookList;
